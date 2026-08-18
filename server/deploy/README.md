# 生产部署与安全加固

> 服务器端同样用于**项目演示**，但已按生产标准加固。本目录包含本地数据迁移产物与一键部署脚本。

## 目录内容

| 文件 | 说明 |
|---|---|
| `huiji-db.sql` | 本地 MySQL 迁移导出（已脱敏：**排除 wx_account 的 app_secret/mch_key/api_v3_key 等密钥数据**，仅保留表结构） |
| `deploy-prod.sh` | 服务器端一键脚本：建库 → 建最小权限账号 → 导入数据 → 注入环境变量 → 启动 |
| `.env.prod.example` | 本地生成的强随机 JWT_SECRET / DB_PASSWORD 样例（**勿提交、勿外传**） |
| `.gitignore` | 上述敏感/数据文件一律不入 git |

## 部署步骤（服务器上）

```bash
# 1. 上传三端产物 + 数据 + 脚本到服务器（scp）
scp server/target/huiji-server-1.0.0.jar server/deploy/huiji-db.sql server/deploy/deploy-prod.sh root@<服务器IP>:/opt/huiji/
# admin/dist、h5/dist 上传到 Nginx 站点目录

# 2. 服务器上执行（先设置 MySQL root 密码与 H5 域名）
export MYSQL_ROOT_PWD='<服务器MySQL root密码>'
export H5_DOMAIN='https://h5.your-domain.com'
cd /opt/huiji && bash deploy-prod.sh

# 3. Nginx: /api 反代到 127.0.0.1:8081; admin/dist、h5/dist 静态托管
```

## 安全加固清单（已落实/需落实）

**代码侧（已落实）**
- `application-prod.yml`：关闭演示数据初始化、关闭短信验证码回显、`sms-code` 置空
- 数据库连接/`JWT_SECRET`/`H5_DOMAIN`/`CORS_ORIGINS` 全部走环境变量注入，代码零明文
- 迁移数据脱敏：`wx_account` 密钥不随库导出（微信支付/公众号凭据到服务器后台重新录入）

**部署侧（deploy-prod.sh 已落实）**
- 数据库使用**专用账号** `huiji_app`（仅对 `huiji` 库 SELECT/INSERT/UPDATE/DELETE 等业务权限，不用 root 跑业务）
- 强随机 `JWT_SECRET`（64 字符 base64）与 `DB_PASSWORD`，`.env` 文件 `chmod 600`
- `CORS_ORIGINS` 留空 = 仅同源（经 Nginx 反代），最严格

**上线后必须人工完成（高优先级）**
1. 修改默认管理员密码 `admin/123456`（后台 → 个人信息 → 修改密码）
2. 在「系统设置」重新配置微信支付商户号、公众号 AppSecret（迁移时已清空，防泄露）
3. MySQL 仅监听内网/防火墙放行；`MYSQL_ROOT_PWD` 执行后及时清理 shell 历史
4. 备份策略：定期 `mysqldump` 全库；本脚本导入前已保留原始 dump 可回滚
5. 如需关闭 H2 控制台/多余端口确认：生产 profile 不启用 H2 console（仅 test 用）
6. HTTPS：Nginx 配置证书，H5_DOMAIN 使用 `https://`

## 常见问题
- **首次启动自动建表**：`ddl-auto: update` 会补齐 `Store` 新增的经纬度等字段，无需手工 ALTER
- **验证码登录**：生产走真实短信网关（需在设置里配短信签名），本地固定码 `8888` 在生产不生效
- **微信功能**：未配置真实公众号时接口优雅降级，不报 500

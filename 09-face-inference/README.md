# MES 人脸登录本机验收

人脸登录默认关闭。普通 MES 按原来的 Java、Node.js、MySQL 和 Redis 流程启动。

## 准备

1. 备份验收库，选择该库执行 04-database/face_login.sql。
2. 使用 Python 3.13 64 位创建本机虚拟环境，在 09-face-inference 下执行 pip install -r requirements.txt。
3. 为 Python 和 Spring Boot 设置同一个随机 MES_FACE_SECRET，至少 32 个字符。启动 app.py，默认监听 127.0.0.1:8091。
4. Spring Boot 设置 MES_FACE_ENABLED=true。MES_FACE_ENDPOINT 默认是 http://127.0.0.1:8091。
5. Spring Boot 设置 MES_FACE_KEY 为独立生成的 32 字节 Base64 密钥。重启时沿用同一个值，否则已有模板需要重新录入。密钥和本机配置留在仓库之外。
6. 显式设置 MES_FACE_THRESHOLD 和 MES_FACE_MARGIN。隔离联调可分别从 0.80 和 0.10 开始，真实样本校准后再决定验收值。
7. 将 02-frontend-web/.env.example 复制为同目录的 .env.local，然后重启 npm run dev。Vite 每次启动会读取这个文件。打开 localhost:5173/login，摄像头使用本机回环地址或 HTTPS。

## Windows 本机启动

从仓库根目录执行以下命令。第一次安装依赖后，日常启动只执行最后一行，并保持推理终端运行。

```powershell
py -3.13 -m venv 09-face-inference/.venv
.\09-face-inference\.venv\Scripts\python.exe -m pip install -r 09-face-inference/requirements.txt
powershell -NoProfile -ExecutionPolicy Bypass -File .\09-face-inference\Start-FaceInference.ps1
```

推理脚本读取当前终端的 MES_FACE_SECRET，也可读取本地 01-backend/config/application.properties 中的 mes.face.secret。Spring Boot 和前端仍从本仓库各自的目录启动。已有模板需沿用原密钥。

## 验收

- 用密码登录，在个人设置的安全中心将面部对准白底头形扫描区。录入成功提示应在三秒后消失，刷新后应保留开通状态和最近录入时间。解绑后应显示未开通。
- 退出后直接点击人脸登录。识别成功应显示姓名与工号，三秒后进入对应岗位页面。
- 检查识别到的工号，测试异人、多人、照片、视频重放以及解除绑定后的拒绝。
- 检查推理服务不可用时密码登录可用。恢复推理服务后重新录入或登录。
- 记录成功率、误认、误拒、响应时间分位数、并发数和测试样本条件。首次模型权重下载单独计时。
- 人脸模板保存在验收库，验证完成后通过安全中心解绑。

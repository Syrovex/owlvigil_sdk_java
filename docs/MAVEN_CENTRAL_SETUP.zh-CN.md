# Maven Central 发布配置

本文档供仓库管理员配置 OwlVigil Java SDK 的 Maven Central 发布环境。
请勿把任何真实凭据提交到仓库、Issue、PR、聊天记录或普通配置文件中。

## 前置条件

- 在 Sonatype Central Portal 中验证命名空间 `com.owlvigil`。
- 为发布账号生成 Central Portal User Token。
- 准备用于 Maven 制品签名的 ASCII-armored GPG 私钥及其密码。
- 对 `Syrovex/owlvigil_sdk_java` 仓库拥有配置 Environment 和 Secret 的权限。

## GitHub Environment

在 GitHub 仓库中进入：

`Settings` → `Environments` → `New environment`

创建名称完全一致的 Environment：

```text
maven-central
```

建议启用 Required reviewers，避免推送版本 Tag 后未经审批直接发布。

## Environment Secrets

在 `maven-central` Environment 中添加以下四个 Secret：

| Secret 名称 | 填写内容 | 来源与用途 |
| --- | --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal User Token 的 Username | Maven Central 上传和发布认证；不是登录邮箱 |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal User Token 的 Password | 与上述 Username 配套；不是网页登录密码 |
| `GPG_PRIVATE_KEY` | 完整的 ASCII-armored GPG 私钥 | 为 JAR、POM、Sources 和 Javadoc 制品生成签名 |
| `GPG_PRIVATE_KEY_PASSWORD` | GPG 私钥密码 | 在 CI 中解锁私钥完成签名 |

客户配置时请按下面四项逐一填写。右侧尖括号中的文字是填写说明，不能作为
Secret 的实际值：

```text
MAVEN_CENTRAL_USERNAME=<粘贴 Central Portal User Token 的 Username>
MAVEN_CENTRAL_PASSWORD=<粘贴 Central Portal User Token 的 Password>
GPG_PRIVATE_KEY=<粘贴从 BEGIN 到 END 的完整 ASCII-armored GPG 私钥>
GPG_PRIVATE_KEY_PASSWORD=<粘贴生成该 GPG 私钥时设置的密码>
```

需要客户提供或完成的实际值清单：

```text
MAVEN_CENTRAL_USERNAME=
MAVEN_CENTRAL_PASSWORD=
GPG_PRIVATE_KEY=
GPG_PRIVATE_KEY_PASSWORD=
```

以上空白清单只能用于确认四项是否已准备好。客户应将真实值直接粘贴到
GitHub Environment Secrets 页面，不要把填入真实值的清单保存或发回。

`GPG_PRIVATE_KEY` 必须保留完整的多行内容，包括：

```text
-----BEGIN PGP PRIVATE KEY BLOCK-----
...
-----END PGP PRIVATE KEY BLOCK-----
```

不要在此文件中用真实值替换示例，也不要创建包含这些值的 `.env` 文件。

## 发布前确认

- `com.owlvigil` Namespace 状态为 Verified。
- 四个 Secret 均配置在 `maven-central` Environment，而不是写入仓库。
- GPG 公钥已上传到公共密钥服务器，且可通过 Key ID 查询。
- `main` 分支的 CI 和 Security 工作流全部通过。
- `build.gradle.kts`、README 和 `CHANGELOG.md` 的版本一致。
- 发布者已明确批准不可覆盖的目标版本，例如 `0.2.0`。

完成配置后，由维护者创建并推送签名的注释 Tag（例如 `v0.2.0`）。
Tag 会触发 `.github/workflows/release.yml`，自动构建、签名、上传并请求
Maven Central 发布。不要通过普通分支 Push 测试正式发布流程。

如果首次发布暂时不使用 GitHub Actions，可在干净的 `main` 分支上运行：

```bash
./scripts/publish-maven-central.sh
```

首次运行时，脚本通过 macOS Keychain 的安全输入保存 Central User Token 和
GPG 密码；后续运行会从登录钥匙串读取，不需要重复输入，也不会创建明文凭据
文件。完成构建与测试后，必须输入小写 `yes` 明确确认才会上传。脚本退出
时会清理相关环境变量。正式版本发布后不可覆盖，因此不要使用真实版本测试
该脚本。

脚本会在显示最终确认前，使用与正式发布相同的 Gradle Signing 配置完成一次
本地制品签名。私钥密码错误或私钥不匹配时会在上传前停止，不会创建 Central
Portal Deployment。

如需撤销本机保存的发布凭据，运行：

```bash
./scripts/publish-maven-central.sh --forget-credentials
```

## 凭据轮换

如果 Central Portal Token 或 GPG 私钥疑似泄露，应立即停止发布、撤销
Token、轮换对应 GitHub Secret，并按项目安全策略处理签名密钥。

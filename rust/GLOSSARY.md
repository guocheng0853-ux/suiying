# Glossary

| Domain (zh / Java) | Meaning | Rust name |
| --- | --- | --- |
| 影视仓 / TVBox | Config + jar/JS spider sites | `tvbox` / spider |
| 线路组件 | Downloaded spider jar/bytes | `component` |
| 原因码 | User/log failure label without secrets | `failure_label` / code |
| `size bytes=N` | Cap or empty body | `size` code + byte count |
| `checksum` | MD5 mismatch | `checksum` |
| `md5-format` | `;md5;` not 32 hex | `md5_format` |
| `classname` | CSP api not `csp_Ident` | `classname` |
| `empty` / `empty bytes=N` | Blank spider payload | `payload_fault` |
| Rules | Pure static policy, no Android | `rules` package / `*_rs` crates |
| Facade | Package-private Java wrapper | stays in `mediapocket` |
| Shadow | Drop-in JNI wrapper from rust-anything | `shadow` |

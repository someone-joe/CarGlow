# 接口自动化测试用例（API）

> 覆盖 `openapi.yaml` 全部 172 个接口，按模块分 A~Q 共 17 段。
> 优先级：P0 = 开业必需；P1 = 体验/效率；P2 = 本期不覆盖。
> 约定：`{token}` = 会员 token；`{wtoken}` = 师傅 token；`{atoken}` = 后台 token；错误码见各条预期。
> 数据基线见 `../README.md` 第四节；每条用例执行前后按需复位格口/产能。

## 目录

| 段 | 模块 | 条数 |
| --- | --- | --- |
| A | 认证与鉴权 | 8 |
| B | 基础数据（站点/商品/机柜/车辆/车位） | 14 |
| C | 下单（产能/格口/优惠券/幂等） | 14 |
| D | 支付（mock/余额/回调幂等） | 11 |
| E | 取消与紧急取钥匙 | 10 |
| F | 订单查询（Tab/时间轴/越权） | 8 |
| G | 开箱码与柜机回调 | 10 |
| H | 订单状态机（17 事件穿测 + 拦截 + 后台白名单） | 14 |
| I | 影像（上传/归属/鉴权） | 8 |
| J | 评价 / 售后 / 退款 | 12 |
| K | 优惠券与保险 | 10 |
| L | 自助洗车 | 12 |
| M | 钱包 / 邀请 / 发票 / 通知 | 9 |
| N | 师傅端 | 12 |
| O | 后台管理 | 14 |
| P | 定时任务 | 7 |
| Q | 安全与合规 | 8 |
| | **合计** | **181** |

---

## A. 认证与鉴权

依据：openapi `auth`/`site`/`misc` tag；PRD 6.1.1 全局交互规范；技术方案 3.2 三套鉴权体系。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-A01 | C 端登录-正向 | P0 | 后端 dev 模式 | `POST /api/v1/auth/login` `{code:"dev"}` | 返回 token 与会员信息；`wash_member` 落库；openid = `dev-openid` | openapi auth/login |
| API-A02 | C 端登录-空 code | P0 | — | `POST /api/v1/auth/login` `{}` | 10001 参数错误 | openapi 入参必填 |
| API-A03 | 无 token 访问需鉴权接口 | P0 | — | 不带 Authorization 请求 `GET /api/v1/orders` | 10002 | 技术方案 3.2 |
| API-A04 | 伪造/过期 token | P0 | — | `Authorization: Bearer fake` 请求 `GET /api/v1/orders` | 10002 | 技术方案 3.2 |
| API-A05 | 会员 token 访问师傅接口 | P0 | 会员 token | `{token}` 请求 `GET /api/v1/worker/profile` | 拒绝（非 worker 安全链，10002/401） | 技术方案 3.2 三套鉴权 |
| API-A06 | 师傅 token 访问 C 端接口 | P1 | 师傅 token | `{wtoken}` 请求 `GET /api/v1/orders` | 拒绝 | 技术方案 3.2 |
| API-A07 | 协议富文本 | P0 | — | `GET /api/v1/auth/agreements` | 返回服务条款/隐私协议富文本（非空） | PRD 6.1.1 |
| API-A08 | 客服配置 | P0 | — | `GET /api/v1/config/customer-service` | 返回双电话/企微码/FAQ；电话取参数配置非硬编码 | PRD 6.1.3 P16 |

## B. 基础数据

依据：openapi `site`/`goods`/`cabinet`/`vehicle`/`park` tag；PRD 6.1.2 页面清单。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-B01 | 当前站点与小区 | P0 | `{token}` | `GET /api/v1/site/current` | 返回站点名/营业时间（19:00-23:00 可配） | openapi site/current |
| API-B02 | 小区列表 | P0 | `{token}` | `GET /api/v1/communities` | 返回已开通小区 | openapi communities |
| API-B03 | 小区开通申请 | P1 | `{token}` | `POST /api/v1/community-applies` 合法入参 | 申请落库；重复申请按幂等/拒绝策略一致 | PRD 6.7.2 营销 |
| API-B04 | 首页 Banner | P0 | `{token}` | `GET /api/v1/banners` | 返回启用中 Banner（含雨天模式同步场景，见 API-P06） | PRD 9 雨天模式 |
| API-B05 | 今晚剩余产能 | P0 | `{token}` | `GET /api/v1/capacity` | 返回站点上限与剩余；值 = Redis `capacity:{siteId}:{date}` | 技术方案 3.4 |
| API-B06 | 服务分类与服务列表 | P0 | `{token}` | `GET /api/v1/service-categories`、`GET /api/v1/services` | 双分类（洗车/美容）；价格单位分；含上下架过滤 | PRD 5 服务列表 |
| API-B07 | 服务详情 | P0 | 有效 serviceId | `GET /api/v1/services/{id}` | 返回明细（items/notIncluded/notices/sampleImages/cover） | openapi services/{id} |
| API-B08 | 服务详情-不存在 | P1 | 非法 id | `GET /api/v1/services/999999` | A0001 | openapi |
| API-B09 | 机柜列表 | P0 | `{token}` | `GET /api/v1/cabinets` | 返回柜机+空闲格口数；`full` 由后端判定 | PRD 6.1.2 P4 |
| API-B10 | 车辆 CRUD 正向 | P0 | `{token}` | `POST /api/v1/vehicles` → `GET` → `PUT` → `DELETE` | 四步均成功；默认车唯一性保持 | PRD 6.1.3 P6 |
| API-B11 | 车辆-越权修改/删除 | P0 | 他人 vehicleId | `PUT/DELETE /api/v1/vehicles/{他人id}` | A0001（越权与不存在统一口径） | PRD 12 安全 |
| API-B12 | 车辆-车牌格式/重复 | P1 | `{token}` | 非法车牌格式；重复车牌 | 均拒绝并提示（错误码以契约为准） | PRD 6.1.3 P6 |
| API-B13 | 常用停车位置 CRUD | P1 | `{token}` | `GET/POST/PUT/DELETE /api/v1/park-locations` | 四步成功；越权同 API-B11 口径 | 拆解 10.3 |
| API-B14 | 免登录接口 | P1 | 无 token | `GET /api/v1/selfwash/pricing` | 200（契约标注免登录） | openapi selfwash |

## C. 下单（产能/格口/优惠券/幂等）——并发热点

依据：技术方案 3.4（三个并发热点）；PRD 9 超产能；openapi `order`。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-C01 | 下单-正向 | P0 | `{token}`、有效 service/vehicle/cabinet、幂等键 K1 | `POST /api/v1/orders` | 订单号返回；状态 `WAIT_PAY`；格口 `RESERVED`；产能 DECR；价格快照（分） | PRD 4.1 |
| API-C02 | 下单-幂等键重复提交 | P0 | 同 K1 再提交一次 | 重复 `POST /api/v1/orders`（同 K1） | 返回**同一订单号**；不重复占格口/产能 | 技术方案 P5 |
| API-C03 | 下单-同内容不同幂等键 | P1 | 同参数、新幂等键 K2 | 再次提交 | 生成新订单（或按契约拒绝重复内容，以契约为准） | openapi 幂等键语义 |
| API-C04 | 下单-产能为 0 | P0 | 将 `capacity:{siteId}:{date}` 置 0 | 提交订单 | A0006（10006）；不建单、不占格口 | PRD 9 超产能 |
| API-C05 | 下单-无空闲格口 | P0 | 占满该柜全部格口 | 提交订单 | B2003；订单回滚（无残留单） | 技术方案 3.4 |
| API-C06 | 下单-缺必填参数 | P0 | — | 依次缺 serviceId/vehicleId/appointTime 提交 | 10001 | openapi 入参 |
| API-C07 | 下单-未同意协议 | P1 | agreed=false | 提交订单 | 拒绝（契约有该字段时） | PRD 6.1.1 |
| API-C08 | 下单-停车照超量 | P1 | parkPhotos > 6 | 提交订单 | 拒绝（`wash.order.park-photo-max` 走配置） | PROGRESS 9.2 |
| API-C09 | 下单-停车照归属回填 | P0 | 先传 PARK 照片（无订单号）再下单 | `POST /api/v1/media/upload`（PARK）→ 下单带 fileIds | 订单生成后 media.order_no 回填；他单抢不走 | PROGRESS E-2 |
| API-C10 | 下单-使用满减券 | P0 | 可用券（满 3900 减 300） | 下单带 couponUserId | `payAmount = 3900-300 = 3600`；`couponDiscount=300`；券 `USED` | PROGRESS 22 |
| API-C11 | 下单-优惠券不可用 | P0 | 用已用/他人/下架券 | 下单带该 couponUserId | C1003；**不下单**（无订单、不占格口） | PROGRESS 22 |
| API-C12 | 最优券计算 | P1 | 多张可用券 | `GET /api/v1/coupons/best?amount=3900` | 返回优惠最大且满足门槛的一张 | openapi coupons/best |
| API-C13 | 下单-预约时间非法 | P1 | 已过时段/非整点 | 提交订单 | 拒绝；时段来自站点配置（19:00-23:00） | PROGRESS 10.6 |
| API-C14 | 下单-跨站点/小区数据 | P1 | 选 B 小区机柜 | 提交订单 | 订单 site_id/community_id 与所选一致 | CODEBUDDY SSOT |

## D. 支付

依据：技术方案 3.4 回调幂等；PRD 9 重复支付/回调丢失；openapi `pay`。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-D01 | mock-pay-正向 | P0 | `WAIT_PAY` 订单 | `POST /api/v1/payments/{orderNo}/mock-pay` | `WAIT_PAY→WAIT_KEY`；写 `wash_payment`；订阅消息触发 | 规则表 PAY_SUCCESS |
| API-D02 | 重复支付 | P0 | 已支付订单 | 再次 mock-pay | 21002；不重复入账 | PROGRESS 7 |
| API-D03 | 支付-订单不存在 | P0 | 假单号 | mock-pay | 21001 | PROGRESS 7 |
| API-D04 | 支付-状态不符 | P0 | `WASHING` 订单 | mock-pay | 21002（仅 WAIT_PAY 可入账） | 技术方案 3.4 |
| API-D05 | 余额支付-充足 | P1 | 余额 ≥ 订单额 | `POST /api/v1/payments/{orderNo}/balance` | 扣款成功 → `WAIT_KEY`；流水落库 | openapi pay/balance |
| API-D06 | 余额支付-不足 | P1 | 余额 < 订单额 | balance 支付 | 拒绝；订单仍 `WAIT_PAY`；不扣款 | openapi pay/balance |
| API-D07 | 预支付参数 | P1 | `WAIT_PAY` 订单 | `POST /api/v1/payments/{orderNo}/prepay` | 返回 prepay 参数（mock 渠道） | openapi pay/prepay |
| API-D08 | 支付回调-验签失败 | P0 | 构造非法签名 notify | `POST /api/v1/payments/wechat/notify` | 拒绝 + 告警；状态不变 | 技术方案 3.4 |
| API-D09 | 回调-重复通知幂等 | P0 | 同 trade_no 发两次 | 两次 notify | 第二次按幂等忽略；仅一次入账 | 技术方案 3.4 |
| API-D10 | 回调-丢失补偿 | P1 | 模拟回调丢失 | 等主动查单补偿任务（5 分钟） | 订单被补偿推进到 `WAIT_KEY` | PRD 9 |
| API-D11 | mock 关闭时的行为 | P2 | `mock-enabled=false` | mock-pay | 拒绝（配置验证，交付前检查项） | PROGRESS 12 |

## E. 取消与紧急取钥匙

依据：规则表 2.3 取消 13 条；PRD 9 客户临时要用车。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-E01 | 取消-未支付 | P0 | `WAIT_PAY` 订单 | `POST /api/v1/orders/{orderNo}/cancel` `{reason}` | → `CANCELED`；格口释放；产能 INCR 回补 | 规则表 2.3 |
| API-E02 | 取消-已支付未取车（KEY_IN） | P0 | `KEY_IN` 订单 | cancel | → `CANCELED`；触发全额退款流程；推客户 | 规则表决策 2 |
| API-E03 | 取消-PICKING 退全款 | P0 | `PICKING` 订单 | cancel | 全额退款，不扣跑腿费；权益返还 | 规则表决策 2 |
| API-E04 | 取消-已开洗被拒（红线） | P0 | `WASHING` 订单 | cancel | 21002；PRD 红线生效 | PRD 9 |
| API-E05 | 取消-不填原因 | P0 | 任意可取消态 | cancel `{}` | 21004 | PROGRESS 7 |
| API-E06 | 取消-订单不存在 | P1 | 假单号 | cancel | 21001 | PROGRESS 7 |
| API-E07 | 重复取消 | P1 | 已 `CANCELED` | cancel | 21002 | 规则表 2.3 |
| API-E08 | 紧急取钥匙-车未动 | P0 | `KEY_IN` 订单 | `POST /api/v1/orders/{orderNo}/emergency-take-key` | 告警 + 取消 + 全额退款 + 留痕 | PRD 9 |
| API-E09 | 紧急取钥匙-车已动 | P0 | `TO_STATION` 及之后 | emergency-take-key | B1002 拒绝 | PRD 9 |
| API-E10 | 取消-权益返还核对 | P1 | 用券订单取消 | cancel 后查券状态 | 券返回 `UNUSED`；体验名额返还 | 规则表 2.3 |

## F. 订单查询

依据：openapi `order` 查询；规则表 3.7 时间轴；PRD 6.1.3 P9/P11。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-F01 | 订单列表-三 Tab | P0 | 造各状态订单若干 | `GET /api/v1/orders?tab=PENDING/SERVING/DONE` | PENDING=待支付+待存钥匙；SERVING=KEY_IN~RETURNED；DONE=待评价/完成/取消/退款中/已退款 | PROGRESS 10.6 |
| API-F02 | 旧 tab 值兼容 | P1 | 老包参数 | `tab=ONGOING/WAIT_REVIEW/ALL` | 兼容返回（映射唯一真源在后端） | PROGRESS 10.6 |
| API-F03 | 非法 tab | P0 | — | `tab=XXX` | 10001 | PROGRESS 1 |
| API-F04 | 订单详情-8 节点时间轴 | P0 | 跑完主链路的订单 | `GET /api/v1/orders/{orderNo}` | 8 节点齐全；节点直接读 `wash_order_status_log`；当前节点高亮 | 规则表 3.7 |
| API-F05 | 详情-字段完整 | P0 | 同上 | 详情返回 | siteName/cabinetName/promiseReturnTime/workerName 等非空（有值时） | PROGRESS 15.1 |
| API-F06 | 详情-越权 | P0 | 他人订单 | 用 `{token}` 查 | 21001（越权与不存在统一） | PRD 12 |
| API-F07 | 订单按钮（action） | P0 | 各状态订单各一 | 列表/详情返回的 action | 按状态返回 PAY/DEPOSIT_KEY/TAKE_KEY/VIEW_PROGRESS/REVIEW/REORDER/CANCEL/CONTACT_SERVICE；前端不推断 | PROGRESS 15 |
| API-F08 | 分页参数 | P1 | >20 单 | `pageNum/pageSize` | 从 1 开始；总数正确 | PROGRESS 4 |

## G. 开箱码与柜机回调

依据：规则表 DEPOSIT_KEY/TAKE_KEY_BACK；PRD 6.2 钥匙柜；附录 B 设备接口。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-G01 | 取开箱码-存钥匙 | P0 | `WAIT_KEY` 订单 | `POST /api/v1/orders/{orderNo}/open-code` | 返回开箱码；10 分钟有效；`wash_slot_open_log` 留痕（码脱敏） | PROGRESS 11 |
| API-G02 | 取开箱码-非允许状态 | P0 | `WASHING` 订单 | open-code | 拒绝（仅 WAIT_KEY/RETURNED 可用） | PROGRESS 11 |
| API-G03 | 取开箱码-越权 | P0 | 他人订单 | open-code | 21001 | PRD 4.3 权限 |
| API-G04 | 柜机回调-存钥匙正向 | P0 | `WAIT_KEY` + 有效码 | `POST /device-callback/v1/slot/deposit` | 格口 `OCCUPIED`；订单 `WAIT_KEY→KEY_IN`；写开箱记录；触发自动派单 | 规则表 2.1 |
| API-G05 | 柜机回调-错误开箱码 | P0 | 随机码 | deposit | 拒绝；状态不变 | PRD 6.2 |
| API-G06 | 柜机回调-重复回调 | P0 | 同码第二次 | deposit | 幂等忽略（已是 KEY_IN） | 技术方案 P5 |
| API-G07 | 柜机回调-格口状态不符 | P1 | 格口非 RESERVED | deposit | 拒绝并告警 | 附录 B |
| API-G08 | 师傅取钥匙开箱码 | P0 | 已派单师傅 | `POST /api/v1/pick/orders/{orderNo}/open-code` + take-key | `KEY_IN→PICKING`；人单绑定校验；留痕 | 规则表 2.1 |
| API-G09 | 客户确认取回钥匙 | P0 | `RETURNED` 订单 | `POST /api/v1/orders/{orderNo}/take-key-back` | `RETURNED→WAIT_REVIEW`；记录取钥匙时间（准点率考核） | 规则表 2.1 |
| API-G10 | 非本人格口开箱 | P0 | A 会员码开 B 格口 | 模拟 | 拒绝；异常开柜告警 | 拆解 4.3/4.4 |

## H. 订单状态机（核心）

依据：规则表全文；`OrderStateMachineTest`（已有 15 个单测）；PRD 附录 A。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-H01 | 17 事件全链路穿测 | P0 | 新订单 | 依次驱动：mock-pay→deposit→take-key→pick-car-done→arrive→sop-done→qc-pass→leave→return-done→take-key-back→reviews | 每步状态符合规则表；终态 `FINISHED`；日志 11+ 条 | 规则表 2.1 |
| API-H02 | 重洗回路 | P0 | `QC` 订单 | qc-fail（填原因）→ 回到 `WASHING` → sop-done → qc-pass | `QC→WASHING` 自环正确；rewash_count+1 | 规则表 2.1/3.2 |
| API-H03 | 重洗 ≥2 次复盘 | P1 | 连续 2 次 qc-fail | 第 2 次 qc-fail | 自动生成复盘工单/告警（当前为 WARN，验证行为与文档一致） | PRD 9 |
| API-H04 | 非法流转拦截 | P0 | 各状态订单 | 规则表外组合（如 `WAIT_PAY + TAKE_KEY`） | 状态机拒绝；状态不变；不写错误日志 | 规则表 2 |
| API-H05 | 终态保护 | P0 | `REFUNDED` 订单 | 任意事件 | 全部拒绝（唯一无出边状态） | 规则表 1 |
| API-H06 | FINISHED 可退款 | P0 | `FINISHED` 订单 | 售后申请退款 | `FINISHED + APPLY_REFUND → REFUNDING` 放行（曾因 isTerminal 被误拦，回归必测） | 规则表决策 1 |
| API-H07 | 流转日志完整性 | P0 | 穿测订单 | 查 `wash_order_status_log` | 每条含 from/to/event/operator_type/source/reason/时间；只增不删 | 规则表 3.7/4 |
| API-H08 | 并发双流转防护 | P1 | `WAIT_PAY` 订单 | 并发 10 次 mock-pay | 仅 1 次成功（Redis 锁 + CAS）；其余 21002 | 技术方案 3.3 |
| API-H09 | 取车照片数量强校验 | P0 | `PICKING` 订单 | pick-car-done 传 <6 张 | 拒绝（≥6 张 360°+仪表盘） | 规则表 2.1 |
| API-H10 | 归柜照片 bizType 校验 | P0 | `RETURNING` 订单 | return-done 传 PICK 类型 fileId | B1005（错配必拦，曾踩坑） | PROGRESS E-2 |
| API-H11 | 后台触发-白名单内 | P0 | `{atoken}`、KEY_IN 订单 | `POST /admin-api/wash/order/{orderNo}/advance`（TAKE_KEY + reason） | 成功；日志 source=ADMIN | 规则表 3.1 |
| API-H12 | 后台触发-白名单外 6 事件 | P0 | 各状态订单 | 依次试 PAY_SUCCESS/APPLY_REFUND/REFUND_SUCCESS/REVIEW_SUBMIT/AUTO_FINISH/CANCEL | 全部 B1002 拒绝 | 规则表 3.1 |
| API-H13 | 后台高危事件二次确认 | P0 | `{atoken}`、RETURNED 订单 | advance TAKE_KEY_BACK 不带 confirm | 拒绝；带 `confirm=true` 后通过；`confirmRequired` 由 event-options 下发 | 规则表 3.1 |
| API-H14 | 后台干预-空原因 | P0 | `{atoken}` | advance 不带 reason | A0001 拒绝 | 规则表 3.1 |

## I. 影像

依据：PROGRESS E-2；技术方案 5.4；PRD 6.1.3 P7。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-I01 | 上传-正向 | P0 | `{token}` | `POST /api/v1/media/upload`（各 bizType） | 返回 fileId；落库；大小/类型受配置约束 | openapi media |
| API-I02 | 上传-超限 | P1 | >10MB 文件 | upload | 拒绝（`wash.media.max-size-mb`） | PROGRESS 9.2 |
| API-I03 | 归属校验-非本单影像 | P0 | 他单 fileId | 推进订单时传入 | B1005 | PROGRESS E-2 |
| API-I04 | 归属校验-本人未绑定影像 | P1 | 本人 PARK 未绑定 | 传入他单场景 | 按 `requireOwnParkPhotos` 规则处理 | PROGRESS E-2 |
| API-I05 | 读取原文件-正向 | P0 | 已上传 fileId | `GET /api/v1/media/{fileId}/raw` | 返回文件流 | openapi media/raw |
| API-I06 | 读取-路径穿越 | P0 | fileId=`../../etc/passwd` | raw | 拒绝（路径穿越校验） | PROGRESS 17 |
| API-I07 | 读取-query token 鉴权 | P0 | 无 header 场景 | `?token=` 带令牌访问 | 放行；无令牌/假令牌拒绝 | PROGRESS 9.2 |
| API-I08 | 师傅影像独立鉴权 | P1 | `{wtoken}` | `POST /api/v1/worker/media/upload` + raw | worker 链独立放行/拒绝 | PROGRESS 9.3 |

## J. 评价 / 售后 / 退款

依据：规则表 2.4 退款；PRD 9.5 退款规则；PROGRESS 13 退款闭环。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-J01 | 评价-正向 | P0 | `WAIT_REVIEW` 订单 | `POST /api/v1/orders/{orderNo}/reviews` `{rating:5}` | → `FINISHED`；一单一评 | 规则表 2.1 |
| API-J02 | 评价-≤4 星强制原因 | P0 | `WAIT_REVIEW` 订单 | rating=3 不填原因 | 拒绝；填原因后成功且告警/复盘 | 规则表 2.1 |
| API-J03 | 评价-评分越界 | P1 | rating=0/6 | reviews | 拒绝（1-5） | 拆解 9.1 |
| API-J04 | 重复评价 | P1 | 已评价订单 | reviews | 拒绝（一单一评） | PROGRESS 17 |
| API-J05 | 非待评价状态评价 | P1 | `WASHING` 订单 | reviews | 21002 | 规则表 |
| API-J06 | 售后-三类建单 | P0 | `FINISHED` 订单 | `POST /api/v1/after-sales`（REWASH/REFUND/CLAIM） | 生成 AF 工单号；只建单不动钱 | PROGRESS 17 |
| API-J07 | 售后-退款受理闭环 | P0 | REFUND 工单 | 后台处置 accept=true | 退款单 SUCCESS → `APPLY_REFUND`+`REFUND_SUCCESS` → 订单 `REFUNDED` → 工单 `DONE` | PROGRESS 13.1 |
| API-J08 | 重复处置幂等 | P0 | 已受理工单 | 再次 accept | 拒绝（`status='INIT'` 乐观锁） | PROGRESS 13.2 |
| API-J09 | 退款成功单重退 | P0 | SUCCESS 退款单 | 人工重试 | 拒绝（绝不重复打款） | PROGRESS 13.2 |
| API-J10 | 驳回不填原因 | P0 | 任意工单 | accept=false 无原因 | 拒绝 | PROGRESS 13.3 |
| API-J11 | CLAIM 不触发退款 | P1 | CLAIM 工单 | 处置同意 | 只开工单/理赔，不动退款单 | PROGRESS 13.3 |
| API-J12 | 部分退款不改状态 | P1 | 构造部分退 | 生成部分退款单 | 订单状态不变（仍 FINISHED） | 规则表 3.1 |

## K. 优惠券与保险

依据：PROGRESS 21/22；openapi `coupon`/`insurance`。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-K01 | 领券中心 | P1 | `{token}` | `GET /api/v1/coupons/center` | 剩余库存 = 总量-已领 | PROGRESS 21 |
| API-K02 | 领券-正向 | P1 | 有库存模板 | `POST /api/v1/coupons/templates/{id}/receive` | 领取成功；库存-1 | PROGRESS 21 |
| API-K03 | 领券-已领完/下架 | P1 | 无库存/下架模板 | receive | C1001 | PROGRESS 21 |
| API-K04 | 领券-超限领 | P1 | 已达 perLimit | receive | C1002 | PROGRESS 21 |
| API-K05 | 我的券-EXPIRED 为计算值 | P1 | 过期 UNUSED 券 | `GET /api/v1/coupons` | 展示 EXPIRED；库内仍 UNUSED | PROGRESS 14.2 |
| API-K06 | 保险产品列表 | P1 | `{token}` | `GET /api/v1/insurances/products` | 返回模板（含 id=1 车损险） | PROGRESS 21 |
| API-K07 | 投保-正向 | P1 | 本人车辆 | `POST /api/v1/insurances` | 保单 ACTIVE；保障期一年；保费 990 分 | PROGRESS 15.3 |
| API-K08 | 投保-他人车辆 | P1 | 他人 vehicleId | insurances | 拒绝（仅本人车辆） | PROGRESS 21 |
| API-K09 | 投保-模板不存在 | P1 | 假 templateId | insurances | C2001 | PROGRESS 21 |
| API-K10 | 投保-车辆必填 | P1 | 缺 vehicleId | insurances | C2002 | PROGRESS 21 |

## L. 自助洗车

依据：openapi `selfwash`（11 接口全量）；拆解 3.1；PRD 5.1 自助订单状态。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-L01 | 计价参数与工位 | P0 | 免登录 | `GET /api/v1/selfwash/pricing`、`/workstations` | 返回档位价格/时长；工位实时状态（空闲/服务中/排队） | 拆解 3.1.1 |
| API-L02 | 选位下单 | P0 | `{token}`、空闲工位 | `POST /api/v1/selfwash/orders` | 建 `WAIT_PAY`（待支付）单；快照服务价/时长 | openapi selfwash |
| API-L03 | 余额支付 | P0 | 待支付单 | `POST .../pay-balance` | 扣余额 → 待使用 | 拆解 3.1.3 |
| API-L04 | 取消-待支付 | P0 | 待支付单 | `POST .../cancel` | 直接取消 | openapi selfwash/cancel |
| API-L05 | 取消-待使用退余额 | P0 | 储值支付的待使用单 | cancel | 退余额；状态已取消 | openapi selfwash/cancel |
| API-L06 | 取消-服务中 | P0 | SERVING 单 | cancel | 拒绝（服务中不可取消） | openapi selfwash/cancel |
| API-L07 | 激活-空闲通电 | P0 | 待使用单+空闲工位 | `POST .../activate` | 通电；生成倒计时；状态 SERVING | 拆解 3.1.6 |
| API-L08 | 激活-前方未完结排队 | P0 | 工位服务中 | activate | 进入排队；不可开始 | 拆解 3.1.5/3.1.6 |
| API-L09 | 加时计费 | P0 | SERVING 单 | `POST .../add-time` | 按「超时价/超时时长」整档计费，不足一档按一档；扣余额；倒计时增加 | openapi selfwash/add-time |
| API-L10 | 结束服务 | P0 | SERVING 单 | `POST .../finish` | 断电完成；有排队则自动开始下一单 | 拆解 3.1.6 |
| API-L11 | 详情-剩余秒数 | P1 | SERVING 单 | `GET /api/v1/selfwash/orders/{orderNo}` | 返回剩余秒数 | openapi selfwash 详情 |
| API-L12 | 我的自助订单列表 | P1 | 多单 | `GET /api/v1/selfwash/orders` | 列表/筛选正确 | 拆解 3.1.4 |

## M. 钱包 / 邀请 / 发票 / 通知

依据：openapi `wallet`/`invite`/`invoice`/`notify`；拆解 7/8/10.6/11。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-M01 | 钱包余额 | P1 | `{token}` | `GET /api/v1/wallet` | 余额/赠送余额分列 | 拆解 7 |
| API-M02 | 充值规则 | P1 | `{token}` | `GET /api/v1/wallet/recharge-rules` | 充赠档位（200送20/300送50/500送100，以配置为准） | 拆解 7.2 |
| API-M03 | 充值-自定义金额下限 | P1 | amount=50 分 | `POST /api/v1/wallet/recharge` | 拒绝（最低 ¥1，服务端校验） | PROGRESS 11.4 |
| API-M04 | 充值到账 | P1 | 充值单 | `POST /api/v1/wallet/recharge/{rechargeNo}/mock-pay` | 到账；流水落库 | openapi wallet |
| API-M05 | 钱包流水分页 | P1 | 多笔 | `GET /api/v1/wallet/flows` | 分页正确；金额单位分 | openapi wallet |
| API-M06 | 邀请码生成/绑定/列表 | P1 | 两个会员 | `GET /api/v1/invite/code`、`POST /invite/bind`、`GET /invite/list` | 首次生成落库；绑定双方权益；统计正确 | 拆解 8.2 |
| API-M07 | 发票申请-金额后端解析 | P1 | 已完成订单 | `POST /api/v1/invoices`（不带金额） | 按订单实付开票；前端传金额无效 | openapi invoice |
| API-M08 | 发票列表/详情 | P1 | 多申请 | `GET /api/v1/invoices`、`/{id}` | 正确返回 | openapi invoice |
| API-M09 | 通知模板/自测推送 | P2 | dev | `GET /api/v1/notify/templates`、`POST /notify/test` | 模板枚举→中文名；x-dev-only 可推 | PRD 6.5 |

## N. 师傅端

依据：openapi `worker`/`master-pick`/`master-station`；PRD 6.3/6.4；PROGRESS 9。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-N01 | 工号登录-正向 | P0 | W001 | `POST /api/v1/worker/login` | 返回 worker token（独立 Redis 链） | PROGRESS 9.1 |
| API-N02 | 工号登录-错误口令 | P0 | 错误口令 | worker/login | 拒绝（D 段错误码） | PROGRESS 9.1 |
| API-N03 | 工号登录-工号不存在 | P1 | 假工号 | worker/login | D4006 | PROGRESS D-1 |
| API-N04 | 档案聚合 | P0 | `{wtoken}` | `GET /api/v1/worker/profile` | 打卡/资质/在班/进行中单量 | openapi worker |
| API-N05 | 打卡上下班 | P0 | `{wtoken}` | `POST /api/v1/worker/work-status` | 在班状态翻转；排班判定生效 | 拆解 12.1 |
| API-N06 | 资质提交/查询/重提 | P0 | `{wtoken}` | `POST/GET /api/v1/worker/qualification` | 未提交空态 submitted=false；重提覆盖 | openapi worker |
| API-N07 | 任务池/待派池 | P0 | 已派单+待派单 | `GET /api/v1/pick/tasks`、`/pick/pool` | 待取/待送分组；候选优先同小区 | PROGRESS T13 |
| API-N08 | 抢单 | P0 | 待派单池有单 | `POST /api/v1/pick/orders/{orderNo}/claim` | 派单成功；重复抢单被拒 | 拆解 12.2 |
| API-N09 | 取车拍照完成 | P0 | `PICKING` 订单 | `POST /api/v1/pick/orders/{orderNo}/pick-car-done`（≥6 照片+bizType=PICK） | → `TO_STATION`；影像归属校验通过 | PROGRESS E-2 |
| API-N10 | 作业台全流程 | P0 | `TO_STATION` 订单 | arrive → sop-done → qc-pass → leave | 依次 `WASHING→QC→WAIT_RETURN→RETURNING` | 规则表 2.1 |
| API-N11 | 归柜还车 | P0 | `RETURNING` 订单 | `POST /api/v1/pick/orders/{orderNo}/return-done`（RETURN 类型） | → `RETURNED`；停放照+归柜留痕 | 规则表 2.1 |
| API-N12 | 师傅端 token 失效行为 | P1 | 过期 wtoken | 任意师傅接口 | 10002；前端跳工号登录页（不自动续登） | PROGRESS 9.2 |

## O. 后台管理

依据：openapi `admin-*`（E-1 已全量入契约）；PRD 6.7；PROGRESS 各后台实测。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-O01 | 后台登录 | P0 | admin/admin123 | `POST /login`（不是 /admin-api/login） | 返回若依 token | PROGRESS 9.4 坑 |
| API-O02 | 无权限访问 | P0 | 无 `wash:order:list` 角色 | `GET /admin-api/wash/order/list` | 403/无权限 | PRD 6.7.3 |
| API-O03 | 订单列表/筛选 | P0 | `{atoken}` | list（status/site/memberId 过滤） | 过滤正确；分页平铺 `{total,rows}` | PROGRESS 1/8 |
| API-O04 | 状态下拉选项 | P0 | `{atoken}` | `GET /admin-api/wash/order/status-options` | 选项来自 OrderStatus 枚举（前端不写死） | PROGRESS 8 |
| API-O05 | 事件选项与白名单一致 | P0 | 各状态订单 | `GET .../event-options` | 仅返回白名单 11 事件；`confirmRequired` 随事件下发 | 规则表 3.1 |
| API-O06 | 会员/车辆列表脱敏 | P0 | `{atoken}` | member/list、vehicle/list | 手机号 `138****1234`、openid `dev-****enid` | PROGRESS 10 |
| API-O07 | 服务项 CRUD+逻辑删除 | P0 | `{atoken}` | 增/改/删 + 详情回查 | 删除后历史订单仍可读服务名快照（不可物理删） | PROGRESS 10.4 |
| API-O08 | 数据看板口径 | P1 | 造各状态单 | `GET /admin-api/wash/dashboard/stats` | 在洗=去程+清洗+待质检；异常=退款中；与订单数据误差 0 | PROGRESS 12 |
| API-O09 | 师傅 CRUD | P1 | `{atoken}` | 增/改/删/reset-password | 工号不允许改；出参不含 password；删除/禁用/重置吊销登录态 | PROGRESS D-1 |
| API-O10 | 工号占用 | P1 | 重复工号 | 新增师傅 | D4005 | PROGRESS D-1 |
| API-O11 | 资质审核/人脸登记 | P1 | PENDING 资质 | `PUT .../qualification/review`、`/face` | 状态流转；派单准入生效 | PROGRESS T13 |
| API-O12 | 周排班读写 | P1 | `{atoken}` | `GET/PUT .../schedule` | 整体覆盖保存；在班判定 | PROGRESS T13 |
| API-O13 | 手动派单/改派 | P0 | 待派单池 | `GET dispatch/pool`、`/candidates`、`POST dispatch` | 派单/改派生效（worker_id 1→2 已验证路径） | PROGRESS T13 |
| API-O14 | 站点/小区/优惠券/Banner/工位后台 CRUD | P1 | `{atoken}` | 各模块 list/add/edit/remove | 全部走契约；编辑不接受 total/issued（券）；perLimit 对后台发放生效 | PROGRESS 10/14 |

## P. 定时任务

依据：PRD 6.7.6 任务清单；技术方案 3.5；规则表超时规则。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-P01 | 支付超时取消 | P0 | 16 分钟前的 `WAIT_PAY` 单 | 等 OrderPayTimeoutJob（1 分钟/轮） | → `CANCELED`（JOB 触发）；格口释放+产能回补 | 规则表 3.4 |
| API-P02 | 格口预占释放 | P0 | 过期 RESERVED 格口 | 等释放任务（5 分钟/轮） | 格口回 `FREE`；可再下单 | 技术方案 3.5 |
| API-P03 | 未存钥匙顺延 | P0 | 23:30 前 `WAIT_KEY` 单 | 触发顺延任务 | 自环 `WAIT_KEY→WAIT_KEY`；改预约日；释放格口重分配；返还券/名额；推送 | 规则表 2.2 |
| API-P04 | T+3 自动完成 | P1 | 3 天前 `WAIT_REVIEW` 单 | 触发 AUTO_FINISH | → `FINISHED`（JOB 触发） | 规则表 2.1 |
| API-P05 | 产能初始化 | P0 | 清空 Redis 产能 key | 触发 CapacityInitJob | 今天起 7 天 key 生成；SETNX 幂等；不重置已扣减值 | PROGRESS 19 |
| API-P06 | 柜机离线检测 | P1 | 心跳超时柜机 | 触发检测任务 | 标记离线 + 企微告警（应急开箱路径可用） | PRD 附录 B |
| API-P07 | 退款重试 | P1 | INIT 失败退款单 | 触发 RefundRetryJob | 5 分钟一轮；上限 5 次；成功后订单收尾 | PROGRESS 13 |

## Q. 安全与合规

依据：PRD 12 安全；技术方案 8 安全与合规；CODEBUDDY 红线 7（留痕）。

| 编号 | 标题 | 优先级 | 前置/数据 | 步骤 | 预期 | 依据 |
| --- | --- | --- | --- | --- | --- | --- |
| API-Q01 | 水平越权-订单 | P0 | 他人订单/车辆/券/影像 | 用 `{token}` 逐一访问 | 全部按「不存在」口径拒绝（21001/A0001） | PRD 12 |
| API-Q02 | 垂直越权-会员打后台 | P0 | `{token}` | 访问 `/admin-api/**` | 401/403 | PRD 12 |
| API-Q03 | 垂直越权-师傅打后台 | P0 | `{wtoken}` | 访问 `/admin-api/**` | 401/403 | PRD 12 |
| API-Q04 | 敏感字段脱敏 | P0 | 后台各列表 | 断言手机号/openid 模式 | `138****1234` / `dev-****enid` | PROGRESS 10/14 |
| API-Q05 | 操作日志只增 | P0 | 后台干预/取消/开箱 | 尝试删除日志（无接口） | 无删除入口；日志持续可查 | CODEBUDDY 红线 7 |
| API-Q06 | 开箱留痕完整 | P0 | 全链路订单 | 查 `wash_slot_open_log` | 每次开箱有记录（谁/何时/哪个格口/码脱敏） | 拆解 4.4 |
| API-Q07 | SQL 注入基础防护 | P1 | 查询参数注入串 | 订单/会员列表模糊查询 | 不报 500、不泄漏、按字面处理 | 技术方案 8 |
| API-Q08 | 影像令牌有效期 | P1 | 过期/无效 query token | 访问 media raw | 拒绝 | PROGRESS 9.2 |

---

## 附：穿测模板（API-H01 展开）

> 全链路穿测（PRD 12 要求 ≥20 单）按此模板执行，每单记录订单号便于回溯：

```
1. 登录拿 {token}
2. POST /api/v1/orders（幂等键 K）→ WAIT_PAY，记录 orderNo/slotNo
3. POST /api/v1/payments/{orderNo}/mock-pay → WAIT_KEY
4. POST /device-callback/v1/slot/deposit（开箱码）→ KEY_IN
5. 师傅：POST /api/v1/pick/orders/{orderNo}/take-key → PICKING
6. 师傅：POST /api/v1/pick/orders/{orderNo}/pick-car-done（≥6 照片）→ TO_STATION
7. 师傅：POST /api/v1/station/orders/{orderNo}/arrive → WASHING
8. 师傅：POST /api/v1/station/orders/{orderNo}/sop-done → QC
9. 师傅：POST /api/v1/station/orders/{orderNo}/qc-pass → WAIT_RETURN
10. 师傅：POST /api/v1/station/orders/{orderNo}/leave → RETURNING
11. 师傅：POST /api/v1/pick/orders/{orderNo}/return-done → RETURNED
12. C 端：POST /api/v1/orders/{orderNo}/take-key-back → WAIT_REVIEW
13. C 端：POST /api/v1/orders/{orderNo}/reviews → FINISHED
14. 核对：GET /api/v1/orders/{orderNo} 时间轴 8 节点全亮 + 日志条数
```

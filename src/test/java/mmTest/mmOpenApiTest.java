package mmTest;

import com.paycrypto.open.api.util.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * MM Open API V2 测试（买卖拆分版）
 *
 * 接口清单（base: /api/v1/mm/customer/asset）：
 *   GET  /list                资产代码列表
 *   GET  /                    查询客户资产（查余额）
 *   GET  /exchange-rate       汇率查询
 *   GET  /exchange/sell/receiver          sell收款人列表（返回的 bank_acct_id 即卖币的 receive_id）
 *   GET  /exchange/buy/whitelist-address  白名单地址列表（返回的 address 即买币的到账地址）
 *   POST /deposit             企业充值（申报 tx_hash）
 *   POST /exchange/sell       卖币下单（新）
 *   POST /exchange/buy        买币下单（新，按花出法币 spend_amount，手续费内扣）
 *   GET  /exchange/history    历史记录（type: deposit/sell/buy，空=全部）
 *   POST /exchange/order      旧合并下单接口（已废弃，仅兼容）
 *
 * 注意：白名单地址、收款人、API Key 均在 Dashboard(admin) 配置；open-api 只提供收款人/白名单地址的只读列表查询。
 */
public class mmOpenApiTest {

    String host = "https://mmapi-sandbox.paycrypto.com";
    private static final String apiKey = "beea25b058f4418dbb7eb6ea2a7d5767";
    private static final String apiSecret = "4de93682-7d65-44af-939d-719fa599b916";
    private static final String apiPassphrase = "123123";

    @Before
    public void setUp() throws Exception {
        HttpUtil.init(host, apiKey, apiSecret, apiPassphrase);
    }

    /**
     * 资产代码列表
     */
    @Test
    public void getAssetCodeList() throws Exception {
        HttpUtil.get("/api/v1/mm/customer/asset/list", "");
    }

    /**
     * 查询客户资产（查余额）
     * currency 用于 total_balance 折算展示币种
     */
    @Test
    public void getCustomerAsset() throws Exception {
        HttpUtil.get("/api/v1/mm/customer/asset", "currency=USD");
    }

    /**
     * 查询汇率
     */
    @Test
    public void getExchangeRate() throws Exception {
        HttpUtil.get("/api/v1/mm/customer/asset/exchange-rate", "from_asset=USDT-TRC20&to_asset=USD");
    }

    /**
     * 历史记录（新 GET 版）
     * type: deposit / sell / buy，不传查全部；时间为毫秒时间戳
     */
    @Test
    public void getExchangeHistory() throws Exception {
        String query = "page_num=1&page_size=20";
        // String query = "page_num=1&page_size=20&former_time=1758464200000&latter_time=1788464200000";
        HttpUtil.get("/api/v1/mm/customer/asset/exchange/history", query);
    }

    /**
     * 企业充值（申报链上 tx_hash）
     * 需要替换为真实、未申报过的 tx_hash
     */
    @Test
    public void postDeposit() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("uni_asset_code", "USDT_TRC20");
        req.put("tx_hash", "62e1ec050564df740b70f12b02d29c5b27ca84ceec6d7d512e37dabdb911d04c");
        HttpUtil.post("/api/v1/mm/customer/asset/deposit", "", JSONObject.toJSONString(req));
    }

    /**
     * sell收款人列表
     * 返回的 bank_acct_id 即卖币下单的 receive_id（仅 status=1 已审核的收款人可用于下单）
     * currency/status 可选过滤
     */
    @Test
    public void getSellReceiverList() throws Exception {
        String query = "page_num=1&page_size=10";
        // String query = "page_num=1&page_size=10&currency=USD&status=1";
        HttpUtil.get("/api/v1/mm/customer/asset/exchange/sell/receiver", query);
    }

    /**
     * 卖币下单（新接口）
     * from_uni_asset_code: 卖出的币；to_asset: 收到的法币；crypto_amount: 卖出币量
     * receive_id 可选：不填默认按币种取已审核收款人（有多个时会报错要求指定）
     *
     * ⚠️ 真实下单、扣真实余额，确认参数后再取消注释运行
     */
    @Test
    public void postSellExchange() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("from_uni_asset_code", "USDT_TRC20");
        req.put("to_asset", "USD");
        req.put("crypto_amount", "10");
        req.put("receive_id", 10777125);
        req.put("source_of_fund", "Business Revenue");
        req.put("purpose_of_trans", "Settlement");
         req.put("remark", "test sell order");
         HttpUtil.post("/api/v1/mm/customer/asset/exchange/sell", "", JSONObject.toJSONString(req));
    }

    /**
     * 白名单地址列表
     * 返回的 address 即买币下单的到账地址 address（仅 status=1 已启用的地址可用于下单）
     * uni_asset_code/status 可选过滤
     */
    @Test
    public void getWhitelistAddressList() throws Exception {
        String query = "page_num=1&page_size=10";
        // String query = "page_num=1&page_size=10&uni_asset_code=USDT_TRC20&status=1";
        HttpUtil.get("/api/v1/mm/customer/asset/exchange/buy/whitelist-address", query);
    }

    /**
     * 买币下单（新接口，按花出法币）
     * from_asset: 付出的法币；to_uni_asset_code: 买入的币；
     * spend_amount: 花出的法币总额（手续费内扣，到账币量 = (spend - fee) / rate）
     * address 可选：收币地址，须已在 Dashboard 白名单审核通过；仅一个可用地址时可省略
     *
     * ⚠️ 真实下单，确认参数后再取消注释运行
     */
    @Test
    public void postBuyExchange() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("from_asset", "USD");
        req.put("to_uni_asset_code", "USDT_ERC20");
        req.put("spend_amount", "1000");
        req.put("address", "0x1234343143431343143431343143431343143431343143431");
        req.put("source_of_fund", "Business Revenue");
        req.put("purpose_of_trans", "Settlement");
         req.put("remark", "test buy order");
         HttpUtil.post("/api/v1/mm/customer/asset/exchange/buy", "", JSONObject.toJSONString(req));
    }

    /**
     * 旧合并下单接口（已废弃 Deprecated，仅验证兼容性）
     * 新接入请使用 /exchange/sell 与 /exchange/buy
     */
    @Test
    public void postLegacyExchangeOrder() throws Exception {
        Map<String, Object> req = new HashMap<>();
        // sell: crypto -> fiat
        req.put("from_uni_asset_code", "USDT_TRC20");
        req.put("to_uni_asset_code", "USD");
        req.put("crypto_amount", "100");
        req.put("source_of_fund", "Business Revenue");
        req.put("purpose_of_trans", "Settlement");
        // HttpUtil.post("/api/v1/mm/customer/asset/exchange/order", "", JSONObject.toJSONString(req));
    }
}

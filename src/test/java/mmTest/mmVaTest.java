package mmTest;

import com.paycrypto.open.api.util.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MM Open API - SGB 虚拟账户（Virtual Account）测试
 *
 * 接口清单（base: /api/v1/mm/va）：
 *   POST /apply/basic      申请虚拟账户 - 第1步 BASIC INFORMATION（个人/企业基本信息 + 身份/企业文件）
 *   POST /apply/financial  申请虚拟账户 - 第2步 FINANCIAL DECLARATION（资金来源/财富来源/税务申报 + 证明文件）
 *   GET  /list             查询当前客户的虚拟账户申请/账户列表（用于轮询审核结果和分配的账号）
 *
 * 注意：
 * 1. basic / financial 两步无先后顺序，任一接口先调用都会创建申请记录（先调 financial 会生成草稿，
 *    后续 basic 补齐），两步按 (客户, 币种) 合并到同一条申请
 * 2. 重复调用为修改：只有 0-待审核 / 2-已拒绝 / 3-开通失败 的申请允许重新提交（会覆盖对应步骤的内容并回到待审核）；
 *    已通过(1)的申请不允许再调用
 * 3. client_type 区分个人(retail)和企业(corporate)，basic 与 financial 两步的 client_type 必须一致
 * 4. documents 需要传真实图片的 base64（jpg/jpeg/png/bmp，单文件 ≤4MB），
 *    服务端上传 S3 后只存 file_url，base64 不会持久化
 * 5. 服务端使用 snake_case 接收参数（spring.jackson.property-naming-strategy=SNAKE_CASE）
 */
public class mmVaTest {

    String host = "https://mmapi-sandbox.paycrypto.com";
    private static final String apiKey = "c80a433eaba54e21a2c26316c6138fa4";
    private static final String apiSecret = "eabca748-0343-4feb-8918-bfb66c82d088";
    private static final String apiPassphrase = "123123";

    private static final String base64img = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADMAAAAyCAMAAADGIxO9AAABdFBMVEUAAAAA//+AgP8zmf9Jkv9Vqv9Nmf9Gov9Aqv9Onf9JpP9Eqv9Qn/9Lpf9Hqv9Dof9Npv9Jnv9Gov9Dpv9Kn/9Ho/9Fp/9Mof9JpP9Gp/9In/9Lpf9Ho/9HoP9KpP9Jof9Hpf9Kov9Go/9Iov9Ho/9Jov9Iov9JpP9Iov9Ho/9Jov9Hof9Iov9Jo/9IpP9Jo/9Iov9Ho/9Iov9Io/9Iov9JpP9Iov9Ho/9Io/9IpP9Hov9IpP9JpP9Io/9Io/9Io/9Io/9Hov9Io/9IpP9Ho/9Jov9Io/9Io/9Io/9IpP9Io/9Io/9Io/9Ho/9Iov9Io/9Io/9Jo/9Io/9Io/9Ho/9Io/9Io/9Io/9Jo/9IpP9Io/9Ho/9Iov9Io/9Io/9Jo/9Io/9IpP9Io/9Jo/9Io/9Io/9Io/9Io/9Jo/9Io/9Io/9Io/9Io/9Io/9Jo/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/9Io/////9nZg6jAAAAenRSTlMAAQIFBwkKCwwNDg8QERITFBUWFxgZGhscHSAiJCstMTY3Ojw9QkdJSktNT1VbXF5gYWNneXt8fYCDhIaJipucpKirra+ztLW3uL3AwsjJysvMzc7P0NHS09TV1tfY2drb3+Dh5OXm5+jp6uvs7vHz9PX3+Pn6+/z9/pduSYEAAAABYktHRHtP0rX8AAACGElEQVRIx5XWWVvTQBQG4K+oiG1jm2ap1n0XRa0b1gVRWVxwAWwbaJZaQBAxuGLw/HovKjqTzILnKk9y3nyTZCbPANnKHbs5PvM+juM4nBq7bGYb7PSJo/d6xNb2i+uDqRafRyeebFOmPt4pcE3EImtcIIiI1m8MsIZ6tZ3jkU2S1ssqayhw+o/e+EWKWj3FGlqqAdjziNT1fYQ1FDpA8a3G0NYwayh0gVJTh76eYQ0t1YDCKx3acFlDgbObpKc51uwyqc4ZCmyg1NKYlSHOUK8GFF5r0Chv6N0hPVof5A35FlBuq9GVlKFuFSiqk56lDXUswPSUs+FA2vST3qjQhYyhRV3S3azRJk0IDC1UgMqC1LREhrouUJyVmTWhoUiFPosNeSZQWRRfSySGPBOwOhLzTYIiFzBmxWZD9qihCxhzQtORvtN2GbB8kZmUfzxJUoL7ilnSKgO2nzXnVdMxdACznTH5LRVqlgA7SBtMkTbJS5tr6qXcLAFOkDJ7V9UoSCclAG5p/k6BzaMEQP6DBs0fBNyQM7hK/5WUAMDAcx2aNwA3Yg2Mng759r/l3jc4+UWH5gyg2mUNhn9qUR6oRqzBuU2NeZDDn+H9NTi9phLJ7X6XG7EGpmLiLZ/d6XIj1gCXliUbg8Y+5tYeZ1BsfBIM6/FxrsntpHZZ++vTP/ilOnpEu3sDhi6OTUdxHMcrMw/rhwVbQvwGOaukvpva6/sAAAAASUVORK5CYII=";

    @Before
    public void setUp() throws Exception {
        HttpUtil.init(host, apiKey, apiSecret, apiPassphrase);
    }

    /**
     * 第1步：提交 BASIC INFORMATION（retail 个人）
     * 必需文件：PROOF_OF_IDENTITY + PROOF_OF_RESIDENTIAL_ADDRESS
     *
     * ⚠️ 请先把 file_base64 替换为真实图片的 base64 再运行
     */
    @Test
    public void postApplyVaRetailBasic() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("currency", "RMB");
        req.put("client_type", "retail");
        req.put("mail", "customer@example.com");
        req.put("nationality", "SGP");
        req.put("country", "SGP");
        req.put("city", "Singapore");
        // 格式 ddMMyyyy，retail 为出生日期
        req.put("date", "01011990");
        // P-护照 N-国民身份证
        req.put("id_type", "P");
        req.put("id_number", "E1234567A");

        // ---------- 个人基本信息 ----------
        req.put("first_name", "John");
        req.put("middle_name", "Michael");
        req.put("last_name", "Doe");
        req.put("contact_number", "+6591234567");
        req.put("occupation", "Software Engineer");
        req.put("address_line1", "1 Raffles Place");
        req.put("address_line2", "20-61");
        req.put("state_province", "Central Region");
        req.put("postal_code", "048616");

        // 身份/住址文件：retail 必需 PROOF_OF_IDENTITY + PROOF_OF_RESIDENTIAL_ADDRESS
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(buildDocument("PROOF_OF_IDENTITY", "passport.jpg",
                base64img));
        documents.add(buildDocument("PROOF_OF_RESIDENTIAL_ADDRESS", "utility_bill.png",
                base64img));
        req.put("documents", documents);

         HttpUtil.post("/api/v1/mm/va/apply/basic", "", JSONObject.toJSONString(req));
    }

    /**
     * 第2步：提交 FINANCIAL DECLARATION（retail 个人）
     * 资金来源/财富来源/税务申报；每个申报的选项建议附 SOURCE_OF_FUNDS_PROOF / SOURCE_OF_WEALTH_PROOF
     *
     * 可与 postApplyVaRetailBasic 调换顺序调用（无先后依赖）
     */
    @Test
    public void postApplyVaRetailFinancial() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("currency", "RMB");
        req.put("client_type", "retail");

        // 资金来源，多选；retail 可选值见文档
        req.put("source_of_funds", Arrays.asList("EMPLOYMENT_INCOME", "INVESTMENTS_OR_TRADING"));
        // 财富来源与资金来源一致时为 true，否则需另传 source_of_wealth
        req.put("source_of_wealth_same_as_funds", true);

        // 税务申报（税收居民辖区），至少一条
        List<Map<String, Object>> taxDeclarations = new ArrayList<>();
        Map<String, Object> tax = new HashMap<>();
        tax.put("country_of_jurisdiction", "SGP");
        taxDeclarations.add(tax);
        req.put("tax_declarations", taxDeclarations);

        // 资金来源/财富来源证明文件（可选，但申报的每个选项建议附一份证明）
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(buildDocument("SOURCE_OF_FUNDS_PROOF", "payslip.jpg",
                base64img));
        req.put("documents", documents);

         HttpUtil.post("/api/v1/mm/va/apply/financial", "", JSONObject.toJSONString(req));
    }

    /**
     * 第1步：提交 BASIC INFORMATION（corporate 企业）
     * 必需文件：CERTIFICATE_OF_INCORPORATION + (ANNUAL_RETURNS_FILING 或 CERTIFICATE_OF_INCUMBENCY)
     *
     * ⚠️ 请先把 file_base64 替换为真实图片的 base64 再运行
     */
    @Test
    public void postApplyVaCorporateBasic() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("currency", "ZAR");
        req.put("client_type", "corporate");
        req.put("mail", "finance@acme.com");
        // 公司注册名
        req.put("full_name", "ACME Trading Pte. Ltd.");
        // corporate：公司注册国
        req.put("country", "SGP");
        req.put("city", "Singapore");
        // 格式 ddMMyyyy，corporate 为公司成立日期
        req.put("date", "15032015");
        // corporate：公司注册号
        req.put("id_number", "201506789K");
        // FI / NON_FI，默认 NON_FI
        req.put("corporate_type", "NON_FI");

        // ---------- 企业联系人 + 注册地址 ----------
        req.put("first_name", "Jane");
        req.put("last_name", "Smith");
        req.put("contact_number", "+6562345678");
        req.put("business_nature", "International trade");
        req.put("address_line1", "10 Anson Road");
        req.put("address_line2", "15-02");
        req.put("state_province", "Central Region");
        req.put("postal_code", "079903");
        // 主要营业地址与注册地址一致；为 false 时需另传 principal_address_line1 / principal_city / principal_country
        req.put("principal_same_as_registered", true);

        // 企业文件：CERTIFICATE_OF_INCORPORATION + (ANNUAL_RETURNS_FILING 或 CERTIFICATE_OF_INCUMBENCY)
        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(buildDocument("CERTIFICATE_OF_INCORPORATION", "coi.jpg",
                base64img));
        documents.add(buildDocument("ANNUAL_RETURNS_FILING", "nar1.jpg",
                base64img));
        req.put("documents", documents);

         HttpUtil.post("/api/v1/mm/va/apply/basic", "", JSONObject.toJSONString(req));
    }

    /**
     * 第2步：提交 FINANCIAL DECLARATION（corporate 企业）
     * 可与 postApplyVaCorporateBasic 调换顺序调用（无先后依赖）
     */
    @Test
    public void postApplyVaCorporateFinancial() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("currency", "ZAR");
        req.put("client_type", "corporate");

        // 资金来源，多选；corporate 可选值见文档
        req.put("source_of_funds", Arrays.asList("BUSINESS_ACTIVITY"));
        req.put("source_of_wealth_same_as_funds", true);

        List<Map<String, Object>> taxDeclarations = new ArrayList<>();
        Map<String, Object> tax = new HashMap<>();
        tax.put("country_of_jurisdiction", "SGP");
        taxDeclarations.add(tax);
        req.put("tax_declarations", taxDeclarations);

        List<Map<String, Object>> documents = new ArrayList<>();
        documents.add(buildDocument("SOURCE_OF_FUNDS_PROOF", "business_proof.jpg",
                base64img));
        req.put("documents", documents);

         HttpUtil.post("/api/v1/mm/va/apply/financial", "", JSONObject.toJSONString(req));
    }

    /**
     * 查询虚拟账户申请/账户列表（轮询审核结果）
     * created_and_assigned: 0-待审核 1-已通过(已分配 virtual_acct_no) 2-已拒绝 3-创建失败
     * mail / currency 均可选
     */
    @Test
    public void getVaList() throws Exception {
        HttpUtil.get("/api/v1/mm/va/list", "currency=USD");
        // HttpUtil.get("/api/v1/mm/va/list", "currency=USD");
    }

    /**
     * 构建 KYC 文件对象；file_url 与 file_base64 二选一（都为空会报错）
     */
    private Map<String, Object> buildDocument(String docType, String fileName, String fileBase64) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("doc_type", docType);
        doc.put("file_name", fileName);
        doc.put("file_base64", fileBase64);
        // doc.put("file_url", "https://xxx.s3.xxx.amazonaws.com/xxx.jpeg");
        return doc;
    }
}

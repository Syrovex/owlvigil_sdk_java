package com.owlvigil;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.owlvigil.core.*;
import com.owlvigil.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedManagementContractTest {
    final ObjectMapper mapper=new ObjectMapper();
    JsonNode spec, manifest;
    @BeforeEach void load() throws Exception {
        spec=mapper.readTree(getClass().getResourceAsStream("/com/owlvigil/management/swagger.json"));
        manifest=mapper.readTree(getClass().getResourceAsStream("/com/owlvigil/management/operations.json"));
    }
    @TestFactory Stream<DynamicTest> allPublicManagementOperations() throws Exception {
        load();Set<String> expected=new TreeSet<>(),actual=new TreeSet<>();
        spec.path("paths").fields().forEachRemaining(p->p.getValue().fields().forEachRemaining(m->expected.add(m.getKey().toUpperCase()+" "+p.getKey())));
        List<DynamicTest> tests=new ArrayList<>();
        for(JsonNode op:manifest){String key=op.path("method").asText()+" "+op.path("path").asText();assertTrue(actual.add(key));tests.add(DynamicTest.dynamicTest(key,()->{ verify(op, 0); verify(op, 1); verify(op, 2); }));}
        assertEquals(144,actual.size());assertEquals(expected,actual);return tests.stream();
    }
    void verify(JsonNode op, int variant) throws Exception {
        boolean minimal = variant != 0;
        String method=op.path("method").asText(),path=op.path("path").asText();
        JsonNode contract=spec.path("paths").path(path).path(method.toLowerCase());
        Map<String,JsonNode> pathValues=new HashMap<>();ObjectNode query=mapper.createObjectNode();JsonNode body=null;
        for(JsonNode parameter:contract.path("parameters")){
            String in=parameter.path("in").asText(),name=parameter.path("name").asText();
            if(in.equals("body"))body=sample(parameter.path("schema"),0);
            else if(in.equals("query") && (!minimal || parameter.path("required").asBoolean()))query.set(name,sample(parameter,0));
            else if(in.equals("path"))pathValues.put(name,parameter.path("type").asText().equals("integer")?LongNode.valueOf(7):TextNode.valueOf("fixture/a b?x"));
        }
        Method target=Arrays.stream(ManagementClient.class.getMethods()).filter(m->m.getName().equals(op.path("javaMethod").asText())&&m.getParameterCount()==op.path("arguments").size()+(minimal?0:1)).findFirst().orElseThrow();
        List<Object> args=new ArrayList<>();int i=0;
        for(JsonNode arg:op.path("arguments")){
            String n=arg.path("name").asText();JsonNode value;
            if(n.equals("query"))value=variant==2 && query.isEmpty()?NullNode.instance:query;else if(n.equals("body"))value=body;
            else {String snake=n.replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase();value=pathValues.get(snake);assertNotNull(value,snake);}
            args.add(mapper.treeToValue(value,target.getParameterTypes()[i++]));
        }
        if(!minimal)args.add(RequestOptions.builder().header("X-Test-Option","preserved").build());
        String code=null;for(Iterator<String> it=contract.path("responses").fieldNames();it.hasNext();){String c=it.next();if(c.startsWith("2")){code=c;break;}}
        assertNotNull(code);JsonNode schema=contract.path("responses").path(code).path("schema").path("properties").path("data");
        JsonNode data=schema.isMissingNode()?NullNode.instance:sample(schema,0);
        try(MockWebServer server=new MockWebServer()){
            MockResponse response=new MockResponse().setResponseCode(Integer.parseInt(code));
            if(!code.equals("204"))response.setHeader("Content-Type","application/json").setBody(mapper.createObjectNode().put("code","ok").put("request_id","fixture-request").set("data",data).toString());
            server.enqueue(response);
            try(OwlVigilClient client=OwlVigilClient.builder().apiKey("fixture-key").managementBaseUrl(server.url("/v1").toString()).maxRetries(0).build()){
                ApiResponse<?> result=(ApiResponse<?>)target.invoke(client.management(),args.toArray());
                assertEquals(Integer.parseInt(code),result.meta().statusCode());
                if(code.equals("204"))assertNull(result.value());else if(!data.isNull())assertNotNull(result.value());
                RecordedRequest request=server.takeRequest(2,TimeUnit.SECONDS);assertNotNull(request);assertEquals(method,request.getMethod());
                if(!minimal)assertEquals("preserved",request.getHeader("X-Test-Option"));assertEquals("Bearer fixture-key",request.getHeader("Authorization"));
                String expectedPath=path;
                for(Map.Entry<String,JsonNode> e:pathValues.entrySet())expectedPath=expectedPath.replace("{"+e.getKey()+"}",java.net.URLEncoder.encode(e.getValue().asText(),java.nio.charset.StandardCharsets.UTF_8).replace("+","%20"));
                assertEquals(expectedPath,request.getRequestUrl().encodedPath());
                for(Iterator<Map.Entry<String,JsonNode>> it=query.fields();it.hasNext();){Map.Entry<String,JsonNode> entry=it.next();JsonNode v=entry.getValue();assertEquals(v.isArray()?v.get(0).asText():v.asText(),request.getRequestUrl().queryParameter(entry.getKey()));}
                if(body!=null)assertJson(body,mapper.readTree(request.getBody().readUtf8()));
                if(result.value()!=null)assertJson(data,mapper.valueToTree(result.value()));
            }
        }
    }
    @Test void invalidPathAndConflictingWorkspaceAreRejectedBeforeSending() throws Exception {
        try (MockWebServer server = new MockWebServer();
             OwlVigilClient client = OwlVigilClient.builder().apiKey("fixture-key")
                 .managementBaseUrl(server.url("/v1").toString()).build()) {
            assertThrows(IllegalArgumentException.class, () ->
                client.management().getGatewayRequestLogsRequestId("  ", null));
            ManagementModels.GetGatewayUsageSummaryQuery query = new ManagementModels.GetGatewayUsageSummaryQuery();
            query.workspaceId = 7L;
            assertThrows(IllegalArgumentException.class, () ->
                client.management().getGatewayUsageSummary(query, RequestOptions.builder().workspaceId(8).build()));
            assertEquals(0, server.getRequestCount());
        }
    }
    @Test void enumsPreserveEveryWireValueAndRejectUnknownValues() throws Exception {
        for (Class<?> type : ManagementModels.class.getDeclaredClasses()) {
            if (!type.isEnum()) continue;
            for (Object value : type.getEnumConstants()) {
                String json = mapper.writeValueAsString(value);
                assertSame(value, mapper.readValue(json, type));
            }
            assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                    () -> mapper.readValue("\"unsupported-enum-value\"", type));
        }
        assertEquals("Usage, Cost & Budget", ManagementModels.AccountSupportRequestIssueType.USAGE_COST_BUDGET.value());
        assertEquals(8, ManagementModels.AccountSupportRequestIssueType.values().length);
        assertEquals(3, ManagementModels.AccessUpdateMemberRequestStatus.values().length);
    }
    void assertJson(JsonNode expected,JsonNode actual){
        if(expected.isNumber()){assertEquals(expected.asDouble(),actual.asDouble());return;}
        if(expected.isObject()){expected.fields().forEachRemaining(e->{assertTrue(actual.has(e.getKey()),e.getKey());assertJson(e.getValue(),actual.get(e.getKey()));});return;}
        if(expected.isArray()){assertEquals(expected.size(),actual.size());for(int i=0;i<expected.size();i++)assertJson(expected.get(i),actual.get(i));return;}
        assertEquals(expected,actual);
    }
    JsonNode sample(JsonNode schema,int depth){
        if(depth>12)return NullNode.instance;
        if(schema.has("$ref"))return sample(spec.path("definitions").path(schema.path("$ref").asText().substring("#/definitions/".length())),depth+1);
        if(schema.has("enum"))return schema.path("enum").get(0);
        switch(schema.path("type").asText()){
            case "integer":return LongNode.valueOf(7);
            case "number":return DoubleNode.valueOf(1.25);
            case "boolean":return BooleanNode.FALSE;
            case "string":return TextNode.valueOf(schema.path("format").asText().equals("date-time")?"2026-09-05T00:00:00Z":"fixture text");
            case "array":return mapper.createArrayNode().add(sample(schema.path("items"),depth+1));
            default:ObjectNode object=mapper.createObjectNode();schema.path("properties").fields().forEachRemaining(e->object.set(e.getKey(),sample(e.getValue(),depth+1)));if(schema.has("additionalProperties"))object.set("custom",sample(schema.path("additionalProperties"),depth+1));return object;
        }
    }
}

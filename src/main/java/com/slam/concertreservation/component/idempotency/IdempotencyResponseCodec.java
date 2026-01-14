package com.slam.concertreservation.component.idempotency;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * IdempotencyRecord 와 ResponseEntity 간의 인코딩/디코딩을 담당하는 컴포넌트
 *
 */
@RequiredArgsConstructor
@Component
public class IdempotencyResponseCodec {

    // IdempotencyRecord 에 저장할 HTTP 헤더의 Allowed List
    private static final Set<String> ALLOWED_HEADERS = Set.of(
            "content-type",
            "location",
            "etag",
            "cache-control"
    );

    private final ObjectMapper objectMapper;

    /**
     * AOP ProceedingJoinPoint 로부터 실제 핸들러 메서드의 가장 구체화된 Type 정보를 반환.
     * <br></br>
     *
     * @param pjp
     * @return
     */
    private Method resolveHandlerMethod(ProceedingJoinPoint pjp){
        MethodSignature signature = (MethodSignature) pjp.getSignature();

        // 1) AOP 시그니처에서 추출한 메서드
        Method method = signature.getMethod();

        // 2) 실제 구현체의 메서드로 재해석
        Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        // 3) 브리지 메서드 반환
        return BridgeMethodResolver.findBridgedMethod(specificMethod);
    }

    /**
     * ResponseEntity 를 IdempotencyRecord 로 인코딩.
     * @param response
     * @return
     */
    public IdempotencyRecord encode(ResponseEntity<?> response){
        if(response == null){throw new IllegalArgumentException("ResponseEntity cannot be null for encoding.");}

        // 1. HTTP Status 추출
        HttpStatusCode statusCode = response.getStatusCode();

        // 2. 허용된 HTTP 헤더만 추출
        Map<String, String> extractedHeaders = extractAllowedHeaders(response.getHeaders());

        // 3. Body 직렬화
        String bodyJson = null;
        Object bodyObject = response.getBody();

        if(bodyObject != null){
            try{
                bodyJson = objectMapper.writeValueAsString(bodyObject);
            } catch (Exception e){
                throw new IllegalStateException("Failed to serialize response body for idempotency record.", e);
            }
        }

        // 4. IdempotencyRecord 생성 및 반환
        return IdempotencyRecord.builder()
                .httpStatusCode(statusCode)
                .headers(extractedHeaders)
                .body(bodyJson)
                .status(IdempotencyRecordStatus.COMPLETED)
                .build();
    }

    public ResponseEntity<?> decode(IdempotencyRecord record, Method handlerMethod){

        // 0. Method Argument 검증
        if(record == null){throw new IllegalArgumentException("IdempotencyRecord cannot be null for decoding.");}
        if(handlerMethod == null){throw new IllegalArgumentException("Handler method cannot be null for decoding idempotency record.");}
        if(record.getHttpStatusCode() == null){throw new IllegalArgumentException("IdempotencyRecord must have a valid HttpStatusCode.");}

        // 1. headers 복원
        HttpHeaders headers = toHttpHeaders(record.getHeaders());

        // 2. handlerMethod 로부터 Body JavaType 추출
        JavaType bodyJavaType = resolveBodyJavaType(handlerMethod);

        // 3. body 역직렬화 Edge Cases I : body 가 존재하지 않음.
        if(Void.class.equals(bodyJavaType.getRawClass())){return new ResponseEntity<>(headers, record.getHttpStatusCode());}

        // 4. body 역직렬화 Edge Cases II : body 가 null 또는 빈 문자열.
        String bodyJson = record.getBody();
        if(bodyJson == null || bodyJson.isBlank() || "null".equals(bodyJson.trim())){
            return new ResponseEntity<>(headers, record.getHttpStatusCode());
        }

        // 5. body 역직렬화 정상 처리.
        final Object bodyObject;
        try {
            bodyObject = objectMapper.readValue(bodyJson, bodyJavaType);
        }
        catch (Exception e){
            throw new IllegalStateException("Failed to deserialize response body from idempotency record. "
                    + "method="
                    + handlerMethod.toGenericString()
                    + ", javaType=" + bodyJavaType, e);
        }

        // 6. ResponseEntity 생성 및 반환.
        return ResponseEntity.status(record.getHttpStatusCode())
                .headers(headers)
                .body(bodyObject);
    }

    private JavaType resolveBodyJavaType(Method handlerMethod){
        if(handlerMethod == null){
            throw new IllegalArgumentException("Handler method cannot be null for resolving body type.");
        }

        // handler 메서드의 반환 타입을 Spring 의 ResolvableType 으로 표현.
        ResolvableType returnType = ResolvableType.forMethodReturnType(handlerMethod);
        Class<?> rawReturnTypeClass = returnType.resolve();

        // ResponseEntity<T> 형태인지 검증.
        if(rawReturnTypeClass == null || !ResponseEntity.class.isAssignableFrom(rawReturnTypeClass)){
            throw new IllegalArgumentException("@Idempotent requires handler return type to be ResponseEntity<T>. "
                    + "method=" + handlerMethod.toGenericString());
        }

        // ResponseEntity<T> 의 제네릭 타입 T 를 추출.
        ResolvableType bodyType = returnType.getGeneric(0);

        // ObjectMapper 에서 인식할 수 있는 JavaType 으로 변환하여 반환.
        return objectMapper.getTypeFactory().constructType(bodyType.getType());
    }

    /**
     * 허용된 HTTP 헤더만 추출하여 Map 으로 반환.
     * @param headers
     * @return
     */
    private Map<String, String> extractAllowedHeaders(HttpHeaders headers){
        Map<String, String> extractedHeaders = new LinkedHashMap<>();

        // Edge Case : headers 가 null 이거나 비어있는 경우 -> 빈 헤더 맵 반환.
        if(headers == null || headers.isEmpty()){
            return extractedHeaders;
        }

        headers.forEach((name, values) -> {

            // Edge Case A : 헤더의 name 또는 values 가 null 이거나 비어있는 경우 무시.
            if(name == null || name.isEmpty() || values == null || values.isEmpty()){return;}
            String normalizedHeader = name.toLowerCase(Locale.ROOT);

            // Edge Case B : 허용된 헤더가 아닌 경우 무시.
            if(!ALLOWED_HEADERS.contains(normalizedHeader)){return;}

            extractedHeaders.put(normalizedHeader, values.get(0));
        });

        return extractedHeaders;
    }

    private HttpHeaders toHttpHeaders(Map<String, String> stored){
        HttpHeaders headers = new HttpHeaders();

        // Edge Case : stored 가 null 이거나 비어있는 경우 -> 빈 HttpHeaders 반환.
        if(stored == null || stored.isEmpty()){
            return headers;
        }

        stored.forEach((name, value) -> {
            // 가공된 헤더의 name 또는 value 가 모두 정상적인 경우에만 추가. (!null && !empty)
            if(name != null && !name.isEmpty() && value != null && !value.isEmpty()){
                headers.add(name, value);
            }
        });

        return headers;
    }
}

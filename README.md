### 페이 - Rest API 기반 결제시스템
   
   ---
   
   ### 개발 프레임워크
   - spring-boot-starter-web:2.2.6.RELEASE
   - spring-boot-starter-data-jpa:2.2.6.RELEASE
   - h2:1.4.200
   - guava:29.0-jre
   - lombok:1.18.12
   - spring-boot-starter-test:2.2.6.RELEASE
   - spock-core:1.3-groovy-2.5
   - spock-spring:1.3-groovy-2.5
   
   
   ### 테이블 설계 
   - 결제 테이블
   ```
   CREATE TABLE IF NOT EXISTS `pay`.`transaction`
   (
       `ID`                          BIGINT(20)   NOT NULL AUTO_INCREMENT,
       `TRANSACTION_ID`              VARCHAR(20)  NOT NULL COMMENT '트랜잭션 아이디',
       `TRANSACTION_TYPE`            VARCHAR(15)  NOT NULL COMMENT '트랜잭션 타입',
       `ENCRYPTED_CARD_INFO`         VARCHAR(300) NOT NULL COMMENT '카드정보',
       `MESSAGE`                     VARCHAR(500) NOT NULL COMMENT '카드사 전문',
       `INSTALLMENT`                 BIGINT(3)    NOT NULL COMMENT '할부 개월 수',
       `AMOUNT`                      BIGINT(20)   NOT NULL COMMENT '결제 금액',
       `VAT`                         BIGINT(20)   NOT NULL COMMENT '부가가치세',
       `REMAIN_AMOUNT`               BIGINT(20)   NOT NULL COMMENT '남은 결제 금액',
       `REMAIN_VAT`                  BIGINT(20)   NOT NULL COMMENT '남은 부가가치세',
       `PAY_TRANSACTION_ID`          VARCHAR(20)  NOT NULL COMMENT '결제 트랜잭션 아이디',
       `CREATED_AT`                  TIMESTAMP    NOT NULL COMMENT '생성일시',
       `UPDATED_AT`                  TIMESTAMP    NULL     COMMENT '수정일시',
       PRIMARY KEY (`ID`),
       CONSTRAINT `UK_TRANSACTION_ID` UNIQUE(`TRANSACTION_ID`),   
       CONSTRAINT `FK_PAY_TRANSACTION_ID` FOREIGN KEY (`PAY_TRANSACTION_ID`) REFERENCES `pay`.`transaction` (`TRANSACTION_ID`)
   );
   ```
   
   - 카드 테이블
   ```
   CREATE TABLE IF NOT EXISTS `pay`.`card`
   (
       `ID`                          BIGINT(20)   NOT NULL AUTO_INCREMENT,
       `MESSAGE`                     VARCHAR(500) NOT NULL COMMENT '카드사 전문',
       PRIMARY KEY (`ID`)
   );
   ```   
   
    
   ### 문제해결 전략
   - 필수 문제
       - 결제 API
           - request validation : @Validated & @Valid
           - process : ConcurrentMap 에 cardNumber 기준으로 락을 잡고 결제 정보 저장  
           ```
           curl -X POST http://localhost:8080/pay/transactions -H "Content-Type: application/json" -d '
           {"cardNumber":"01234567890","expirationMonthYear":"1212","cvc":"123","installment":0,"amount":11000,"vat":1000}
           '
           ```
       - 결제취소 API (transactionId = 결제 트랜잭션 아이디)
           - request validation : @Validated & @Valid
           - process : @OptimisticLocking 을 사용해서 한 결제건에 대해서 동시 취소 방지 
           ```
           curl -X DELETE http://localhost:8080/pay/transactions/{transactionId} -H "Content-Type: application/json" -d '
           {"amount":3000,"vat":300}
           '
           ```
       - 조회 API (transactionId = 결제 트랜잭션 아이디)
           - request validation : @Validated
           - process : transactionId 로 조회
           ```
           curl -X GET http://localhost:8080/pay/transactions/{transactionId} -H "Content-Type: application/json"
           ```
       - API 요청 실패
           - BadRequestException : 잘못 된 요청
           - ConflictException : 트랜잭션 충돌 
           - CryptoFailException : 암호화 실패
           - FieldErrorException : invalid request
           - IllegalStatusException : 요청이 처리 될 수 없는 상태
           - TransactionNotFoundException : 요청을 시도한 대상 트랜잭션이 없음
       
   - 부분 취소 테스트 (선택 문제 부분취소 API를 구현하고 Test Case를 통과시켜주세요.)
       ```
       ./gradlew test --tests 'com.pay.api.integration*'
       ```
       
   - Multi Thread 테스트
       - 결제 : 하나의 카드번호로 동시에 결제를 할 수 없습니다.
           - 방법 : 카드번호를 기준으로 락을 이용하여 해결 
       - 전체취소 : 결제 한 건에 대한 전체취소를 동시에 할 수 없습니다. 
           - 방법 : version column 을 이용한 optimistic lock 을 이용하여 해결
       - 부분취소 : 결제 한 건에 대한 부분취소를 동시에 할 수 없습니다.
           - 방법 : version column 을 이용한 optimistic lock 을 이용하여 해결
       ```
       ./gradlew test --tests 'com.pay.api.multithread*'
       ```
   - 카드 암/복호화 
       ```
       ./gradlew test --tests com.pay.api.domain.CardInfoCryptoTest
       ```
   - 메세지 테스트 
       ```
       ./gradlew test --tests com.pay.api.domain.MessageBuilderTest
       ```
   
   ### 빌드 및 실행하기
   ```
   git clone .git
   cd simple-pay-api
   ./gradlew clean build
   java -jar build/libs/simple-pay-api.jar
   ```
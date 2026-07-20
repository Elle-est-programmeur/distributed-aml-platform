package com.tradesentry.casedata;

import com.tradesentry.proto.casedata.AccountHistoryRequest;
import com.tradesentry.proto.casedata.AccountHistoryResponse;
import com.tradesentry.proto.casedata.CaseDataServiceGrpc;
import com.tradesentry.proto.casedata.SimilarCase;
import com.tradesentry.proto.casedata.SimilarCasesRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * gRPC implementation of the case-data contract. Spring gRPC auto-registers any bean of type
 * {@link io.grpc.BindableService} (which the generated base class implements) with the server.
 *
 * <p>Data is synthetic and deterministic — derived from the accountId hash — mirroring the
 * Phase 5 stub so behaviour is identical now that the call crosses a process boundary.
 */
@Service
public class CaseDataGrpcService extends CaseDataServiceGrpc.CaseDataServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CaseDataGrpcService.class);

    @Override
    public void getAccountHistory(AccountHistoryRequest request,
                                  StreamObserver<AccountHistoryResponse> responseObserver) {
        String accountId = request.getAccountId();
        int hash = Math.abs(accountId.hashCode());
        String riskBand = switch (hash % 3) {
            case 0 -> "LOW";
            case 1 -> "MEDIUM";
            default -> "HIGH";
        };
        boolean hasPriorSar = (hash % 5 == 0);
        int totalTransactions = 50 + hash % 200;
        double avgTransactionAmount = 1000 + hash % 5000;
        double maxTransactionAmount = 20000 + hash % 80000;
        int priorFlags = hash % 6;

        AccountHistoryResponse response = AccountHistoryResponse.newBuilder()
                .setAccountId(accountId)
                .setTotalTransactions(totalTransactions)
                .setAvgTransactionAmount(avgTransactionAmount)
                .setMaxTransactionAmount(maxTransactionAmount)
                .setPriorFlags(priorFlags)
                .setHasPriorSar(hasPriorSar)
                .setRiskBand(riskBand)
                .build();

        log.info("getAccountHistory [account={}] -> band={} priorSar={}",
                accountId, riskBand, hasPriorSar);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void retrieveSimilarCases(SimilarCasesRequest request,
                                     StreamObserver<SimilarCase> responseObserver) {
        int max = Math.max(1, Math.min(request.getMaxResults(), 5));
        boolean risky = request.getAmount() > 20000
                || List.of("KP", "IR", "SY").contains(request.getCounterpartyCountry());
        int count = risky ? max : Math.max(1, max - 2);

        log.info("retrieveSimilarCases [amount={} country={}] streaming up to {}",
                request.getAmount(), request.getCounterpartyCountry(), count);

        for (int i = 0; i < count; i++) {
            String outcome = risky ? (i % 2 == 0 ? "SAR_FILED" : "ESCALATED") : "CLEARED";
            String caseId = "case-" + (1000 + i);
            double similarityScore = 0.95 - i * 0.07;
            String summary = "Prior " + outcome + " case with comparable profile";

            SimilarCase similarCase = SimilarCase.newBuilder()
                    .setCaseId(caseId)
                    .setSimilarityScore(similarityScore)
                    .setOutcome(outcome)
                    .setSummary(summary)
                    .build();

            // Stream each case as it is "found" rather than collecting them first.
            responseObserver.onNext(similarCase);
        }

        responseObserver.onCompleted();
    }
}

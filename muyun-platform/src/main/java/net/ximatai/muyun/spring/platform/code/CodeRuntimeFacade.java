package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CodeRuntimeFacade {
    private final CodeGenerateService generateService;
    private final CodeRuleService ruleService;
    private final CodeLedgerEntryService ledgerEntryService;

    public CodeRuntimeFacade(CodeGenerateService generateService,
                             CodeRuleService ruleService,
                             CodeLedgerEntryService ledgerEntryService) {
        this.generateService = Objects.requireNonNull(generateService, "generateService must not be null");
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService must not be null");
        this.ledgerEntryService = ledgerEntryService;
    }

    public CodeRuntimeResult generateOnly(CodeRuntimeCommand command) {
        GenerateCodeResult generated = generate(command);
        return CodeRuntimeResult.generated(generated, false, null);
    }

    @Transactional
    public CodeRuntimeResult issueAndBind(CodeRuntimeCommand command) {
        if (command == null) {
            throw new PlatformException("Code runtime command must not be null");
        }
        if (command.sourceRecordId() == null || command.sourceRecordId().isBlank()) {
            throw new PlatformException("Code issueAndBind requires sourceRecordId");
        }
        if (ledgerEntryService == null) {
            throw new PlatformException("Code issueAndBind requires ledgerEntryService");
        }
        CodeLedgerEntry existing = ledgerEntryService.findActiveByTargetAndSourceRecord(
                command.moduleAlias(),
                command.entityAlias(),
                command.fieldName(),
                command.sourceRecordId()
        );
        if (existing != null) {
            CodeRule existingRule = ruleService.viewRuleTree(existing.getRuleId());
            return CodeRuntimeResult.bound(existing, existingRule);
        }
        GenerateCodeResult generated = generate(command.withUniquenessChecker(ledgerAwareChecker(command)));
        CodeRule rule = ruleService.viewRuleTree(generated.ruleId());
        ledgerEntryService.upsertActiveBinding(
                rule,
                generated.value(),
                generated.basisKey(),
                generated.periodKey(),
                command.sourceRecordId()
        );
        return CodeRuntimeResult.generated(generated, true, command.sourceRecordId());
    }

    private GenerateCodeResult generate(CodeRuntimeCommand command) {
        if (command == null) {
            throw new PlatformException("Code runtime command must not be null");
        }
        return generateService.generate(command.toGenerateCommand());
    }

    private CodeValueUniquenessChecker ledgerAwareChecker(CodeRuntimeCommand command) {
        return (resolved, generatedValue, context) -> {
            CodeValueUniquenessChecker checker = command.uniquenessChecker();
            if (checker != null && checker.exists(resolved, generatedValue, context)) {
                return true;
            }
            CodeLedgerEntry entry = ledgerEntryService.findByRuleAndValue(resolved.rule().getId(), generatedValue);
            return entry != null
                    && entry.getStatus() == CodeLedgerStatus.ACTIVE
                    && (entry.getSourceRecordId() == null
                    || !entry.getSourceRecordId().equals(command.sourceRecordId()));
        };
    }
}

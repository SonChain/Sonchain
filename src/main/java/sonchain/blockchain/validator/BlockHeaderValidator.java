package sonchain.blockchain.validator;

import java.util.Arrays;
import java.util.List;

import sonchain.blockchain.core.BlockHeader;

/**
 *
 */
public class BlockHeaderValidator extends BlockHeaderRule{

    private List<BlockHeaderRule> m_rules;

    /**
     * Init
     * @param rules
     */
    public BlockHeaderValidator(List<BlockHeaderRule> rules) {
        this.m_rules = rules;
    }

    /**
     * Init
     * @param rules
     */
    public BlockHeaderValidator(BlockHeaderRule ...rules) {
        this.m_rules = Arrays.asList(rules);
    }

    /**
     * validate
     * @param header
     * @return
     */
    @Override
    public ValidationResult validate(BlockHeader header) {
        for (BlockHeaderRule rule : m_rules) {
            ValidationResult result = rule.validate(header);
            if (!result.m_success) {
                return result;
            }
        }
        return Success;
    }
}

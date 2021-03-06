package sonchain.blockchain.consensus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockTimestamp;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.data.BaseMessage;

/**
 * @author GAIA
 *
 */
public class ConsensusContext {

	public final static int Initial = (0x00);
	public final static int Primary = (0x01);
	public final static int Backup = (0x02);
	public final static int RequestSent = (0x04);
	public final static int RequestReceived = (0x08);
	public final static int SignatureSent = (0x10);
	public final static int BlockSent = (0x20);
	public final static int ViewChanging = (0x40);

	public static final Logger m_logger = Logger.getLogger(ConsensusContext.class);
	public final static int Version = 0;
	public int m_state = Initial;		
	public byte[] m_preHash = null;     
	public int m_blockNumber = 0;		
	public int m_viewNumber = 0;		
	public SonChainProducerNode[] m_validators = null;
	public int m_myIndex = -1;			
	public int m_primaryIndex = 0;		
	public BlockTimestamp m_timestamp = null;
	public BigInteger m_nonce = BigInteger.ZERO;
	public String m_nextConsensus = null;	
	public List<Transaction> m_lstTransaction = new ArrayList<Transaction>();
	public ByteArrayMap<Transaction> m_transactions = new ByteArrayMap<Transaction>();
	public byte[][] m_signatures = null;   
	public int[] m_expectedView = null;	   
	public ECKey m_keyPair = null;		  
	public Block m_header = null;
	
	public int getMinAllowCount() {
		int minAllowCount =  m_validators.length - (m_validators.length - 1) / 3;
		m_logger.debug("getMinAllowCount count：" + minAllowCount);
		return minAllowCount;
	}
	
	/**
	 * @param view_number 
	 */
	public void changeView(int view_number){
		int p = (m_blockNumber - view_number) % m_validators.length;
		m_state &= SignatureSent;
		m_viewNumber = view_number;
		m_primaryIndex = p >= 0? p : (p + m_validators.length);
		if(m_state == Initial){
			m_lstTransaction.clear();
			m_transactions.clear();
			m_signatures = new byte[m_validators.length][];
		}
		m_expectedView[m_myIndex] = view_number;
		m_header = null;
	}
	
	/**
	 * @return
	 */
	public ConsensusPayload makeChangeView()
	{
		ChangeViewMessage changeView = new ChangeViewMessage(m_expectedView[m_myIndex], m_viewNumber);
		return makePayload(changeView);
	}
	
	/**
	 * @return
	 */
	public Block makeHeader(){
		if(m_lstTransaction == null || m_lstTransaction.size() == 0){
			return null;
		}
		if(m_header == null){
			//TODO
			//m_header = DataCenter.getSonChainImpl().getBlockChain().createNewBlock(
			//				DataCenter.getSonChainImpl().getBlockChain().getBlockByHash(m_preHash), m_lstTransaction);
					
//					new Block(m_preHash,
//	        		null,
//	        		m_blockNumber,
//	        		m_timestamp,  		// block time
//	                new byte[] {0},  	// extra data
//	                new byte[0],  		// receiptsRoot - computed after running all transactions
//	                DataCenter.m_sonChainImpl.getBlockChain().calcTxTrie(m_lstTransaction),    // TransactionsRoot - computed after running all transactions
//	                new byte[] {0}, 	// stateRoot - computed after running all transactions
//	                m_lstTransaction);  
		}
		return m_header;
	}
	
	/**
	 * @param message
	 * @return
	 */
	private ConsensusPayload makePayload(BaseMessage message){
		ConsensusPayload payload = new ConsensusPayload();
		payload.m_messageType = message.getCommand();
		payload.m_version = Version;
		payload.m_preHash = m_preHash;
		payload.m_blockNumber = m_blockNumber;
		payload.m_validatorIndex = m_myIndex;
		payload.m_timestamp = m_timestamp;
		payload.m_data = message.getEncoded();
		return payload;
	}
	
	/**
	 * @return
	 */
	public ConsensusPayload makePrepareRequest()
	{
		PrepareRequestMessage request = new PrepareRequestMessage(m_nonce, 
				m_nextConsensus, m_lstTransaction, m_header, m_signatures[m_myIndex], m_viewNumber);
		return makePayload(request);	
	}
	
	/**
	 * @param signature
	 * @return
	 */
	public ConsensusPayload makePrepareResponse(byte[] signature){
		PrepareResponseMessage response = new PrepareResponseMessage(signature, m_viewNumber);
		return makePayload(response);	
	}
	
	/**
	 */
	public void reset(){
		m_state = Initial;  
		m_preHash = DataCenter.getSonChainImpl().getBlockChain().getBestBlockHash();
		m_blockNumber = (int)DataCenter.getSonChainImpl().getBlockChain().getBestBlock().getBlockNumber() + 1;
		m_viewNumber = 0;  
		m_validators = DataCenter.getSonChainImpl().getBlockChain().getValidators();
		int validatorLength = m_validators.length;
		m_myIndex = -1; 
		m_primaryIndex = m_blockNumber % validatorLength; 
		m_lstTransaction.clear();
        m_signatures = new byte[validatorLength][];
		m_expectedView = new int[validatorLength]; 
		m_keyPair = null;
		for(int i = 0; i < validatorLength; i++){
			SonChainProducerNode peerInfo = m_validators[i];
			if(peerInfo.getHost().equals(DataCenter.m_config.m_localHost)
					&& peerInfo.getPort() == DataCenter.m_config.m_localPort){
				m_myIndex = i;
				m_keyPair = ECKey.fromPrivate(Hex.decode(DataCenter.m_config.m_localNodePrivateKey));
				break;
			}
		}
		m_header = null;		
	}	
}

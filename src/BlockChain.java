import java.util.ArrayList;
import java.util.Vector;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private Vector _blocks= new Vector(); //We'll have max 20 block nodes
    private TransactionPool _globTxnPool= new TransactionPool();
    
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	_blocks.add(genesisBlock);
    }

    /** Get the maximum height block */
    /**
     * The height of a block is the number of blocks in the chain between it and the genesis block. 
     * (So the genesis block has height 0.) The height of the block chain is usually taken to be the 
     * height of the highest block, in the chain with greatest total difficulty; i.e. the length of 
     * the chain minus one.
     */
    public Block getMaxHeightBlock() {
    	//For now assume just one chain of blocks
    	return (Block) _blocks.lastElement();
    	
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    /**
     * Get all the txns in the globalTxnPool and set the unclaimed outputs
     * into the utxo pool to be mined for the block
     */
    public UTXOPool getMaxHeightUTXOPool() {
    	UTXOPool pool= new UTXOPool();
    	ArrayList txns= _globTxnPool.getTransactions();
    	for (int i=0; i < txns.size(); i++) {
    		Transaction txn= (Transaction) txns.get(i);
    		ArrayList inputs= txn.getInputs();
    		for (int j=0; j < inputs.size(); j++) {
    			Transaction.Input ii= (Transaction.Input) inputs.get(j);
    			System.out.println("Tx#" + txn.hashCode() + " input index=" + ii.outputIndex);
    			UTXO utxo = new UTXO(ii.prevTxHash, ii.outputIndex);
    	   		ArrayList outputs= txn.getOutputs();
        		for (int k=0; k < outputs.size(); k++) {
        			Transaction.Output o= (Transaction.Output) outputs.get(k);
        			pool.addUTXO(utxo, o);
        		}
    		}
    	}
    	
    	return pool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
    	return _globTxnPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * When checking for validity of a newly received block, just checking if the transactions form a
	 *	valid set is enough. The set need not be a maximum possible set of transactions. Also, you
	 *	neednt do any proof-of-work checks.
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
    	if (block == null) return false;
    	
        // A new genesis block wont be mined. If you receive a block which claims to be a genesis block
    	//(parent is a null hash) in the addBlock(Block b) function, you can return false .
    	if (block.getPrevBlockHash() == null) {
    		//System.out.println("#addBlock got a Genesis Block. Ignore.");
    		return false;
    	}
    	
    	//Check that the prevHash block is indeed available
    	byte[] prevHash= block.getPrevBlockHash();
    	boolean found=false;
    	for (int k=0; k < _blocks.size(); k++) {
    		Block bk= (Block) _blocks.get(k);
    		if (bk.getHash() == prevHash) {
    			found=true;
    		}
    	}
    	if (!found) {
    		return false;
    	}
    	
    	ArrayList<Transaction> block_txns= block.getTransactions();
    	for (int i=0; i < block_txns.size(); i++) {
    		Transaction block_txn= block.getTransaction(i);
    		_globTxnPool.removeTransaction(block_txn.getHash());
    	}
    	
    	_blocks.add(block);
    	
    	return true;
    }

    /** Add a transaction to the transaction pool */
    /*
     * Maintain only one global Transaction Pool for the block chain and keep adding transactions to
		it on receiving transactions and remove transactions from it if a new block is received or
		created.
     */
    public void addTransaction(Transaction tx) {
    	_globTxnPool.addTransaction(tx);
    }
}
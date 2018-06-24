import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private Vector _blocks= new Vector(); //We'll have max 20 block nodes
    private HashMap _poolsForBlocks= new HashMap();
    private TransactionPool _globTxnPool= new TransactionPool();
    
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	UTXOPool utxoPool= new UTXOPool();
    	_poolsForBlocks.put(genesisBlock.hashCode(),  utxoPool);
    	addCoinbaseToUTXOPool(genesisBlock, utxoPool);
    	_blocks.add(genesisBlock);
    }

    //pinched
    private void addCoinbaseToUTXOPool(Block block, UTXOPool utxoPool) {
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }
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
    	Block mhBlock= getMaxHeightBlock();
    	System.out.println("#getMaxHeightUTXOPool: mhBlock=" + mhBlock.hashCode());
    	
    	return getUtxoPoolForBlock( mhBlock );
    }

    private UTXOPool getUtxoPoolForBlock(Block block) {
    	UTXOPool pool= (UTXOPool) _poolsForBlocks.get(block.hashCode());
    	if (pool == null) {
    		_poolsForBlocks.put(block.hashCode(),  new UTXOPool());
    	}
    	
    	return (UTXOPool) _poolsForBlocks.get(block.hashCode());
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
    	Block prevBlock= null;
    	byte[] prevHash= block.getPrevBlockHash();
    	boolean found=false;
    	for (int k=0; k < _blocks.size(); k++) {
    		Block bk= (Block) _blocks.get(k);
    		if (bk.getHash() == prevHash) {
    			found=true;
    			prevBlock= bk;
    		}
    	}
    	if (!found) {
    		return false;
    	}
    	
    	TxHandler txHandler= new TxHandler( getUtxoPoolForBlock(prevBlock) ); //note! (pinched) block) );
    	ArrayList<Transaction> block_txns= block.getTransactions();
    	System.out.println("#addBlock : Block=" + block.hashCode() + " has " + block_txns.size() + " Txns");
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = txHandler.handleTxs(txs);
        if (validTxs.length != txs.length) {
            return false;
        }
    	/*for (int i=0; i < block_txns.size(); i++) {
    		Transaction block_txn= block.getTransaction(i);
    		//if (!txHandler.isValidTx(block_txn)) return false;
    	}*/
        addCoinbaseToUTXOPool(block, getUtxoPoolForBlock(block));
        
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
    public void addTransaction(Transaction txn) {
    	_globTxnPool.addTransaction(txn);
    	//for (int i=0; i < txns.size(); i++) {
    		//Transaction txn= (Transaction) txns.get(i);
    	UTXOPool  pool= getMaxHeightUTXOPool();
    		ArrayList outputs= txn.getOutputs();
			for (int j=0; j < outputs.size(); j++) {
				Transaction.Output o= (Transaction.Output) outputs.get(j);
				UTXO utxo = new UTXO(txn.getHash(),j);
				pool.addUTXO(utxo, o);
				System.out.println("#getMaxHeightUTXOPool: addUTXO Value=" + o.value + " at index=" + j + " to txn=" + txn.hashCode());
				System.out.println("UTXO Pool now has: " + pool.getAllUTXO().size() + " Elements");
			}
			
	    	//We've added the new unclaimed txns (UTXOs) - we should remove the earlier ones!
			ArrayList inputs= txn.getInputs();
			for (int j=0; j < inputs.size(); j++) {
				Transaction.Input ii= (Transaction.Input) inputs.get(j);
				UTXO ut= new UTXO(ii.prevTxHash, ii.outputIndex);
				Transaction.Output txOut= pool.getTxOutput(ut);
				if (txOut != null) {
					pool.removeUTXO(ut);
					System.out.println("getMaxHeightUTXOPool: removedUTXO Value=" + txOut.value + " at index=" + ii.outputIndex + " from txn=" + txn.hashCode());
				}
			}
    	//}
    }
}
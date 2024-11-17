package com.example.blockchainjava.Observer;

import com.example.blockchainjava.Model.Block.BlockChain;

public interface BlockchainUpdateObserver {
    void onBlockchainUpdate(BlockChain blockchain);
}

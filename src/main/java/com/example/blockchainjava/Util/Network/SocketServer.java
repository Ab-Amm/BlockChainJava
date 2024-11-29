package com.example.blockchainjava.Util.Network;

import com.example.blockchainjava.Model.Block.Block;
import com.example.blockchainjava.Model.Block.BlockChain;
import com.example.blockchainjava.Model.Transaction.Transaction;
import com.example.blockchainjava.Model.Transaction.TransactionStatus;
import com.example.blockchainjava.Model.User.Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SocketServer {
    private final BlockChain blockchain;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private List<Socket> connectedClients = new ArrayList<>();

    public SocketServer(BlockChain blockchain, Validator validator) {
        this.blockchain = blockchain;
        this.validator = validator;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }



}


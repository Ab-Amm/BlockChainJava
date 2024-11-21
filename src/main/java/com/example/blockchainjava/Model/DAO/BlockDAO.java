package com.example.blockchainjava.Model.DAO;

import com.example.blockchainjava.Model.Block.Block;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockDAO {
    private final Connection connection;

    public BlockDAO() {
        this.connection = DatabaseConnection.getConnection();
    }

    public void saveBlock(Block block) {
        String sql = "INSERT INTO blocks (block_id, previous_hash, current_hash, timestamp, validator_signature) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, block.getBlockId());
            stmt.setString(2, block.getPreviousHash());
            stmt.setString(3, block.getCurrentHash());
            stmt.setTimestamp(4, Timestamp.valueOf(block.getTimestamp()));
            stmt.setString(5, block.getValidatorSignature());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save block", e);
        }
    }

    public List<Block> getAllBlocks() {
        List<Block> blocks = new ArrayList<>();
//        String sql = "SELECT * FROM blocks ORDER BY block_id";
//
//        try (PreparedStatement stmt = connection.prepareStatement(sql);
//             ResultSet rs = stmt.executeQuery()) {
//
//            while (rs.next()) {
//                Block block = new Block(
//                        rs.getString("previous_hash"),
//                        null,  // Transaction will be loaded separately
//                        rs.getString("validator_signature")
//                );
//                // Set other properties
//                blocks.add(block);
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException("Failed to load blocks", e);
//        }
//        return blocks;
        return blocks ;
    }
}
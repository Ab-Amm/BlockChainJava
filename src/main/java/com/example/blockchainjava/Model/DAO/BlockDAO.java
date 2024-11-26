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
        // SQL query excluding the blockId because it will be auto-generated by the database
        String sql = "INSERT INTO blocks (previous_hash, current_hash, timestamp, validator_signature) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, block.getPreviousHash());
            stmt.setString(2, block.getCurrentHash());
            stmt.setTimestamp(3, Timestamp.valueOf(block.getTimestamp()));
            stmt.setString(4, block.getValidatorSignature());

            stmt.executeUpdate();

            // Retrieve the auto-generated blockId (the primary key that was auto-incremented)
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    // Set the auto-generated blockId in the Block object
                    block.setBlockId(rs.getLong(1));  // The first column contains the generated blockId
                }
            }
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
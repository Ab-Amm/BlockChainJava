package com.example.blockchainjava.Controller;

import com.example.blockchainjava.Model.User.Validator;
import javafx.scene.Node;
import javafx.scene.control.Dialog;

import java.security.NoSuchAlgorithmException;

public class ValidatorFormController extends Node {
    public ValidatorFormController(Dialog<Validator> dialog) {
        this.dialog = dialog;
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public void saveValidator() {
        dialog.setResult(validator);
        dialog.close();
    }

    public void cancel() {
        dialog.close();
    }

    private Dialog<Validator> dialog;
    private Validator validator;
    }


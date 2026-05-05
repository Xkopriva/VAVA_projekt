package com.example.bais;
import com.example.bais.controllers.*;
import com.example.bais.models.*;
import com.example.bais.services.*;
import com.example.bais.components.*;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(LoginView.class, args);
    }
}

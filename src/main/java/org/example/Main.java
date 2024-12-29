package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

public class Main {
    private static Controller controller;
    private static JList<String> modelList;
    private static JList<String> dataList;
    private static JTable table;

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Modelling Framework");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Panel for model and data selection
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BorderLayout());
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel title = new JLabel("Select model and data");
        selectionPanel.add(title, BorderLayout.NORTH);

        // Model selection list
        DefaultListModel<String> modelListModel = new DefaultListModel<>();
        loadFilesIntoModel(modelListModel, "src/main/java/models");
        modelList = new JList<>(modelListModel);
        //modelList.setBorder(BorderFactory.createTitledBorder("Select Model"));
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionPanel.add(new JScrollPane(modelList), BorderLayout.WEST);

        // Data selection list
        DefaultListModel<String> dataListModel = new DefaultListModel<>();
        loadFilesIntoModel(dataListModel, "src/main/java/data");
        dataList = new JList<>(dataListModel);
        //dataList.setBorder(BorderFactory.createTitledBorder("Select Data"));
        dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionPanel.add(new JScrollPane(dataList), BorderLayout.EAST);

        JButton runModelButton = new JButton("Run Model");
        selectionPanel.add(runModelButton, BorderLayout.SOUTH);

        // Add selection panel to the frame
        frame.add(selectionPanel, BorderLayout.WEST);

        // Table for displaying data
        JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new BorderLayout());
        viewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.setDefaultRenderer(Object.class, rightRenderer);

        JScrollPane tableScrollPane = new JScrollPane(table);
        viewPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 3));

        JButton runScriptFromFileButton = new JButton("Run Script from File");
        buttonPanel.add(runScriptFromFileButton);

        JButton createAndRunAdHocScriptButton = new JButton("Create and Run Ad Hoc Script");
        buttonPanel.add(createAndRunAdHocScriptButton);

        viewPanel.add(buttonPanel, BorderLayout.SOUTH);
        frame.add(viewPanel, BorderLayout.CENTER);

        // Add functionality to other buttons
        runModelButton.addActionListener(e -> {
            if (modelList.getSelectedValue() != null && dataList.getSelectedValue() != null) {
                controller = new Controller("models." + modelList.getSelectedValue().replace(".java", ""));
                controller.readDataFrom("src/main/java/data/" + dataList.getSelectedValue());
                controller.runModel();
                updateView();
            }
        });

        // Add functionality to "Run Script from File" button
        runScriptFromFileButton.addActionListener((ActionEvent e) -> {
            if(table.getModel().getRowCount() > 0) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Script File");
                fileChooser.setCurrentDirectory(new File("src/main/java/scripts"));
                int result = fileChooser.showOpenDialog(frame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    controller.runScriptFromFile(selectedFile.getAbsolutePath());
                    updateView();
                }
            }
        });

        // Add functionality to "Create and Run Ad Hoc Script" button
        createAndRunAdHocScriptButton.addActionListener((ActionEvent e) -> {
            // Create the dialog
            JDialog scriptDialog = new JDialog(frame, "Script", true);
            scriptDialog.setSize(400, 300);
            scriptDialog.setLayout(new BorderLayout());

            // Text area for script input
            JTextArea scriptArea = new JTextArea();
            scriptArea.setText("DPKB = new double[LL]\nfor (t = 0; t < LL; t++) {\n    DPKB[t] = PKB[t] / PKB[0] * 100\n}");
            scriptDialog.add(new JScrollPane(scriptArea), BorderLayout.CENTER);

            // Panel for OK and Cancel buttons
            JPanel dialogButtonPanel = new JPanel();
            dialogButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            JButton okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");
            dialogButtonPanel.add(okButton);
            dialogButtonPanel.add(cancelButton);

            // Add dialogButtonPanel to dialog
            scriptDialog.add(dialogButtonPanel, BorderLayout.SOUTH);

            // OK button action
            okButton.addActionListener(okEvent -> {
                controller.runScript(scriptArea.getText());
                updateView();

                scriptDialog.dispose();
            });

            // Cancel button action
            cancelButton.addActionListener(cancelEvent -> scriptDialog.dispose());

            // Display the dialog
            scriptDialog.setVisible(true);
        });

        // Display the frame
        frame.setVisible(true);
    }

    private static void oldMain(){
        Controller ct1 = new Controller("models.Model");
        ct1.readDataFrom("src/main/java/data/" + "data2.txt")
                .runModel()
                .runScriptFromFile("src/main/java/scripts/script1.groovy")
                .runScriptFromFile("src/main/java/scripts/script2.groovy");
        String res = ct1.getResultsAsTsv();
        System.out.println(res);
    }

    private static void loadFilesIntoModel(DefaultListModel<String> modelListModel, String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        modelListModel.addElement(file.getName());
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Directory not found: " + directoryPath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void updateView(){
        var tableData = parseTSVString(controller.getResultsAsTsv());
        var columnNames = tableData[0];
        var rows = new Object[tableData.length - 1][tableData[0].length - 1];
        System.arraycopy(tableData, 1, rows, 0, tableData.length - 1);
        DefaultTableModel tableModel = new DefaultTableModel(rows, columnNames);
        table.setModel(tableModel);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
    }

    private static Object[][] parseTSVString(String tsvString) {
        var rows = new ArrayList<String[]>();
        String[] lines = tsvString.split("\n"); // Split by line
        for (String line : lines) {
            rows.add(line.split("\t")); // Split each line by tabs
        }

        // Convert List<String[]> to Object[][]
        int columnCount = rows.getFirst().length;
        Object[][] data = new Object[rows.size()][columnCount];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        return data;
    }
}
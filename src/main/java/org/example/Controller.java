package org.example;

import annotations.Bind;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Controller {

    private Object _instance;
    private double[] _years;
    private Map<String, double[]> scriptVariables;

    public Controller(String modelName) {
        try {
            _instance = Class.forName(modelName).getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        scriptVariables = new LinkedHashMap<>();
    }

    public Controller readDataFrom(String fName){
        Map<String, double[]> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if(line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                String key = parts[0];
                String[] values = Arrays.copyOfRange(parts, 1, parts.length);
                double[] parsedValues = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    parsedValues[i] = Double.parseDouble(values[i]);
                }
                data.put(key, parsedValues);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Field[] fields = _instance.getClass().getDeclaredFields();

        for (Field field : fields) {

            if (field.isAnnotationPresent(Bind.class)) {
                String fieldName = field.getName();

                if (fieldName.equals("LL")) {
                    field.setAccessible(true);
                    try {
                        field.set(_instance, data.get("LATA").length);
                        _years = data.get("LATA");
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Check if data contains the field
                if (data.containsKey(fieldName)) {
                    FillFieldWithCompleteData(data.get(fieldName), field);
                }

                // Check if script variables contain field
                if (scriptVariables.containsKey(fieldName)) {
                    FillFieldWithCompleteData(scriptVariables.get(fieldName), field);
                }
            }
        }

        //reads the data for the calculation of a file named fname
        return this;
    }

    private void FillFieldWithCompleteData(double[] rawData, Field field) {
        var fieldData = new double[_years.length];
        for (int i = 0, lastIndex = 0; i < _years.length; i++) {
            if(rawData.length > i) {
                fieldData[i] = rawData[i];
                lastIndex = i;
            }
            else
                fieldData[i] = rawData[lastIndex];
        }

        field.setAccessible(true);
        try {
            field.set(_instance, fieldData);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Controller runModel(){
        try {
            Method runMethod = _instance.getClass().getMethod("run");
            runMethod.invoke(_instance);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return this;
    }

    public Controller runScriptFromFile(String fName){
        var filePath = Path.of(fName);
        String script;
        try {
            script = Files.readString(filePath);

        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            return this;
        }
        return runScript(script);
    }

    public Controller runScript(String script){
        // Create a Groovy Binding
        Binding binding = new Binding();

        // Bind all fields annotated with @Bind to the Groovy script
        Field[] fields = _instance.getClass().getDeclaredFields();
        ArrayList<String> bindedFieldNames = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Bind.class)) {
                field.setAccessible(true);
                try {
                    binding.setVariable(field.getName(), field.get(_instance));
                    bindedFieldNames.add(field.getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Bind fields created in scripts
        for (Map.Entry<String, double[]> entry : scriptVariables.entrySet()){
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        // Create and run the Groovy script
        GroovyShell shell = new GroovyShell(binding);
        shell.evaluate(script);

        for (var obj : binding.getVariables().entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) obj;

            if(entry.getKey().length() < 2 && entry.getKey().matches("[a-z]"))
                continue;

            if(bindedFieldNames.contains(entry.getKey()))
                continue;

            scriptVariables.put(entry.getKey(), (double[])entry.getValue());
        }
        //executes the script code specified as a string
        return this;
    }

    public String getResultsAsTsv(){
        var outputStringBuilder = new StringBuilder();
        try{
            Field[] fields = _instance.getClass().getDeclaredFields();

            // Write header
            var sb = new StringBuilder();
            sb.append("\t");
            for (double year : _years) {
                sb.append((int)year).append("\t");
            }
            sb.deleteCharAt(sb.length()-1);
            outputStringBuilder.append(sb).append("\n");
            //writer.write(sb.toString());
            //writer.newLine();

            // Write rows for each object

            for (Field field : fields) {
                var rowSB = new StringBuilder();
                if(field.getName().equals("LL"))
                    continue;

                if(!field.isAnnotationPresent(Bind.class))
                    continue;

                field.setAccessible(true);
                rowSB.append(field.getName()).append("\t");
                for (double value : (double[]) field.get(_instance)) {
                    rowSB.append(String.format("%.2f", value)).append("\t");
                }
                //rowSB.deleteCharAt(sb.length()-1);
                outputStringBuilder.append(rowSB).append("\n");
            }

            for (Map.Entry<String, double[]> entry : scriptVariables.entrySet()) {
                var rowSB = new StringBuilder();
                rowSB.append(entry.getKey()).append("\t");
                for (double value : entry.getValue()) {
                    rowSB.append(String.format("%.3f", value)).append("\t");
                }
                outputStringBuilder.append(rowSB).append("\n");
            }
            //writer.write(String.join("\t", row));
            //writer.newLine();

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return outputStringBuilder.toString();
    }
}

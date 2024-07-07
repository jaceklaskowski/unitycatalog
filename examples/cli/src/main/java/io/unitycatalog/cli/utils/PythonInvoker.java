package io.unitycatalog.cli.utils;

import io.unitycatalog.client.ApiException;
import io.unitycatalog.client.model.ColumnTypeName;
import io.unitycatalog.client.model.FunctionInfo;
import io.unitycatalog.client.model.FunctionParameterInfo;
import io.unitycatalog.client.model.FunctionParameterInfos;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonInvoker {

    public static String invokePython(FunctionInfo function, String scriptPath, String ...args) throws ApiException {
        try {
            // Prepare the arguments for the Python script
            List<String> argsList = new ArrayList<>();
            argsList.add("python3");
            argsList.add(scriptPath);

            // Add function name and routine body
            argsList.add(function.getName());
            argsList.add(function.getRoutineDefinition());

            // Retrieve and add parameters as arguments
            List<FunctionParameterInfo> parameters = function
                    .getInputParams()
                    .getParameters();
            if (parameters == null || parameters.isEmpty()) {
                throw new ApiException("Function parameters not found.");
            }
            if (args.length < parameters.size()) {
                throw new ApiException("Not enough parameters provided: " + args.length + ", expected: " + parameters.size());
            }
            List<String> paramNames = new ArrayList<>();
            List<Object> argValues = new ArrayList<>();
            for (FunctionParameterInfo param : parameters) {
                System.out.println(">>> param=" + param);
                paramNames.add(param.getName());
                String argument = args[param.getPosition()];
                if (param.getTypeName().equals(ColumnTypeName.INT)) {
                    argValues.add(Integer.parseInt(argument));
                } else if (param.getTypeName().equals(ColumnTypeName.DOUBLE)) {
                    argValues.add(Double.parseDouble(argument));
                } else if (param.getTypeName().equals(ColumnTypeName.STRING)) {
                    argValues.add(argument);
                } else {
                    throw new ApiException("Unsupported parameter type: " + param.getTypeName());
                }
                // Example values, these should be dynamically provided based on use case
            }

            // Add parameter names and values
            argsList.add(String.join(", ", paramNames));
            argsList.add(new JSONArray(argValues).toString());


            // Invoke the Python script
            ProcessBuilder pb = new ProcessBuilder(argsList);
            Process p = pb.start();

            // Get the output from the Python script
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String resultLine;
            StringBuilder result = new StringBuilder();
            while ((resultLine = in.readLine()) != null) {
                result.append(resultLine).append("\n");
            }
            in.close();
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApiException("Error invoking Python script: " + e.getMessage());
        }

    }

    public static void main(String[] args) throws ApiException {
        // Example usage
        FunctionInfo function = new FunctionInfo();
        function.setName("lowercase");
        function.setRoutineDefinition("return x.lower()");
        FunctionParameterInfo param1 = new FunctionParameterInfo();
        param1.setName("x");
        param1.setTypeName(ColumnTypeName.STRING);
        List<FunctionParameterInfo> params = new ArrayList<>();
        params.add(param1);

/*
        FunctionParameterInfo param2 = new FunctionParameterInfo();
        param2.setName("y");
        param2.setTypeName(ColumnTypeName.INT);
        params.add(param2);
*/
        function.setInputParams(new FunctionParameterInfos().parameters(params));
        System.out.println(invokePython(function, "etc/data/function/python_engine.py" , args));
    }

}

package com.velocityparser.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocityparser.VelocityBranchSimulator;
import com.velocityparser.VelocitySimulator;
import com.velocityparser.VelocityVariableExtractor;
import com.velocityparser.model.BranchResult;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class VelocityCliFacade {

    private static final Gson GSON = new GsonBuilder().create();

    static class CliRequest {
        String action; // "extract", "simulate", "branch"
        String template;
        Map<String, Object> variables;
    }

    static class CliResponse {
        boolean success;
        Object data;
        String errorMessage;
        
        static CliResponse ok(Object data) {
            CliResponse res = new CliResponse();
            res.success = true;
            res.data = data;
            return res;
        }
        
        static CliResponse error(String message) {
            CliResponse res = new CliResponse();
            res.success = false;
            res.errorMessage = message;
            return res;
        }
    }

    public static void main(String[] args) {
        try {
            Reader reader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            CliRequest req = GSON.fromJson(reader, CliRequest.class);

            if (req == null || req.action == null) {
                System.out.println(GSON.toJson(CliResponse.error("Invalid JSON input or missing 'action' field.")));
                return;
            }
            if (req.template == null) {
                req.template = "";
            }

            CliResponse res;
            switch (req.action) {
                case "extract":
                    VelocityVariableExtractor extractor = new VelocityVariableExtractor();
                    List<String> vars = extractor.extract(req.template);
                    res = CliResponse.ok(vars);
                    break;
                case "simulate":
                    VelocitySimulator simulator = new VelocitySimulator();
                    String output = simulator.simulate(req.template, req.variables);
                    res = CliResponse.ok(output);
                    break;
                case "branch":
                    VelocityBranchSimulator branchSim = new VelocityBranchSimulator();
                    List<BranchResult> results = branchSim.simulateAllBranches(req.template);
                    res = CliResponse.ok(results);
                    break;
                default:
                    res = CliResponse.error("Unknown action: " + req.action);
                    break;
            }

            System.out.println(GSON.toJson(res));
            
        } catch (Exception e) {
            System.out.println(GSON.toJson(CliResponse.error("Exception occurred: " + e.getMessage())));
            e.printStackTrace(System.err);
        }
    }
}

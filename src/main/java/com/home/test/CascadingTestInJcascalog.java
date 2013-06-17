
package com.home.test;

import jcascalog.Api;
import jcascalog.Subquery;
import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.hadoop.Lfs;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascalog.CascalogFunction;

import com.twitter.maple.tap.StdoutTap;

public class CascadingTestInJcascalog {

    public void findMapping(int depth,String path) {
        Fields fields = new Fields("line");
        Lfs lfs = new Lfs(new TextLine(fields), path);

        Subquery input = new Subquery("?node", "?mappingNode").predicate(lfs, "?line").predicate(new FieldExtractor(),
                "?node", "?mappingNode");

        Object mappingTillPreviousDepth = findMappingForMultipleDepth(depth - 1, input);

        Subquery subquery = new Subquery("?node", "?newMappingNode").predicate(input, "?node", "?mappingNode")
                .predicate(mappingTillPreviousDepth, "?mappingNode", "?newMappingNode");

        Object newMappings = Api.union(subquery, mappingTillPreviousDepth);
        Api.execute(new StdoutTap(), newMappings);
    }

    private Object findMappingForMultipleDepth(int depth, Subquery input) {
        if (depth == 0)
            return input;
        Object mappingTillPreviousDepth = findMappingForMultipleDepth(depth - 1, input);
        Subquery subquery = new Subquery("?node", "?newMappingNode").predicate(input, "?node", "?mappingNode")
                .predicate(mappingTillPreviousDepth, "?mappingNode", "?newMappingNode");

        return Api.union(subquery, mappingTillPreviousDepth);
    }

   

    public static class FieldExtractor extends CascalogFunction {

        @Override
        public void operate(FlowProcess flowProcess, FunctionCall fnCall) {
            String line = fnCall.getArguments().getString(0);
            String[] fields = line.split(",");
            fnCall.getOutputCollector().add(new Tuple(fields[0], fields[1]));
        }

    }
    
    public static void main(String[] args) {
        int depth = Integer.parseInt(args[0]);
        String path = args[1];
        CascadingTestInJcascalog cascadingTestInJcascalog = new CascadingTestInJcascalog();
        cascadingTestInJcascalog.findMapping(depth,path);

    }

}

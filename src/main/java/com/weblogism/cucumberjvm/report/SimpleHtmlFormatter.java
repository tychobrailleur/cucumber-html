package com.weblogism.cucumberjvm.report;

import gherkin.formatter.Formatter;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;

import cucumber.runtime.CucumberException;

/**
 * @author Sébastien Le Callonnec
 */
public class SimpleHtmlFormatter implements Formatter, Reporter {
    enum Status {
        UNKNOWN, PASSED, FAILED
    }

    public final static String REPORT_HTML = "report.html";
    public final static String[] TEXT_ASSETS = { "style.css" };

    private final File htmlReportDir;
    private NiceAppendable outStream;

    private final VelocityContext context = new VelocityContext();

    private final Summary summary = new Summary();
    private Status status = Status.UNKNOWN;
    
    private final List<Node> features = new ArrayList<Node>();
    private Node currentFeature;
    private Node currentScenario;
    private int currentStep;
    
    private long currentTime = 0;

    public SimpleHtmlFormatter(File htmlReportDir) {
        this.htmlReportDir = htmlReportDir;
        this.currentTime = System.currentTimeMillis();
    }

    /**
     * @see gherkin.formatter.Reporter#result(gherkin.formatter.model.Result)
     */
    public void result(Result result) {
        if (result != null && !Result.UNDEFINED.equals(result)) {
            if ("passed".equals(result.getStatus())) {
                if (status == Status.UNKNOWN) {
                    status = Status.PASSED;
                }
                summary.passedSteps++;
            } else if ("failed".equals(result.getStatus())) {
                status = Status.FAILED;
                summary.failedSteps++;
            }
            
            Node step = this.currentScenario.getChildren().get(currentStep);
            step.setStatus(result.getStatus());

            currentStep++;
        }
    }

    /**
     * @see gherkin.formatter.Reporter#match(gherkin.formatter.model.Match)
     */
    public void match(Match match) {
        // TODO Auto-generated method stub

    }

    /**
     * @see gherkin.formatter.Reporter#embedding(java.lang.String, byte[])
     */
    public void embedding(String mimeType, byte[] data) {
        // TODO Auto-generated method stub

    }

    /**
     * @see gherkin.formatter.Formatter#uri(java.lang.String)
     */
    public void uri(String uri) {
        
    }

    /**
     * @see gherkin.formatter.Formatter#feature(gherkin.formatter.model.Feature)
     */
    public void feature(Feature feature) {
        updateScenarioResultCount();
        summary.features++;
        
        this.currentFeature = new Node(feature);
        features.add(currentFeature);
    }

    /**
     * @see gherkin.formatter.Formatter#background(gherkin.formatter.model.Background)
     */
    public void background(Background background) {
        updateScenarioResultCount();
        summary.scenarios++;
        
        this.currentScenario = new Node(background);
        this.currentFeature.addChild(currentScenario);
    }

    /**
     * @see gherkin.formatter.Formatter#scenario(gherkin.formatter.model.Scenario)
     */
    public void scenario(Scenario scenario) {
        updateScenarioResultCount();
        summary.scenarios++;
        
        this.currentScenario = new Node(scenario);
        this.currentFeature.addChild(currentScenario);
    }

    /**
     * @see gherkin.formatter.Formatter#scenarioOutline(gherkin.formatter.model.ScenarioOutline)
     */
    public void scenarioOutline(ScenarioOutline scenarioOutline) {
    }

    /**
     * @see gherkin.formatter.Formatter#examples(gherkin.formatter.model.Examples)
     */
    public void examples(Examples examples) {
    }

    /**
     * @see gherkin.formatter.Formatter#step(gherkin.formatter.model.Step)
     */
    public void step(Step step) {
        this.currentScenario.addChild(new Node(step));
    }

    /**
     * @see gherkin.formatter.Formatter#eof()
     */
    public void eof() {
        System.out.println("EOF: ");
    }

    /**
     * @see gherkin.formatter.Formatter#syntaxError(java.lang.String, java.lang.String, java.util.List,
     *      java.lang.String, int)
     */
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, int line) {
        // TODO Auto-generated method stub

    }

    /**
     * @see gherkin.formatter.Formatter#done()
     */
    public void done() {
        updateScenarioResultCount();
        copyReportFiles();

        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

        Velocity.init();

        InputStream input = this.getClass().getClassLoader().getResourceAsStream("reportHeader.vm");
        InputStreamReader reader = new InputStreamReader(input);

        context.put("esc", new EscapeTool());
        context.put("features", summary.features);
        context.put("scenarios", summary.scenarios);
        context.put("failedSteps", summary.failedSteps);
        context.put("passedSteps", summary.passedSteps);
        context.put("failedScenarios", summary.failedScenarios);
        context.put("passedScenarios", summary.passedScenarios);
        context.put("duration", (System.currentTimeMillis() - currentTime) / 1000.0);
        context.put("allFeatures", features);

        StringWriter stringWriter = new StringWriter();

        boolean evaluate = Velocity.evaluate(context, stringWriter, "reportHeader", reader);

        if (evaluate) {
            out().append(stringWriter.toString());
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * @see gherkin.formatter.Formatter#close()
     */
    public void close() {
        outStream.close();
    }

    private void updateScenarioResultCount() {
        System.out.println("Current status == " + status);
        
        if (status == Status.FAILED) {
            summary.failedScenarios++;
            this.currentScenario.setStatus("failed");
        } else if (status == Status.PASSED) {
            summary.passedScenarios++;
            this.currentScenario.setStatus("passed");
        }

        this.currentStep = 0;
        status = Status.UNKNOWN;
    }

    private NiceAppendable out() {
        if (outStream == null) {
            try {
                outStream = new NiceAppendable(new OutputStreamWriter(reportFileOutputStream(REPORT_HTML), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new CucumberException(e);
            }
        }
        return outStream;
    }

    private OutputStream reportFileOutputStream(String fileName) {
        File file = new File(htmlReportDir, fileName);
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new CucumberException("Error creating file: " + file.getAbsolutePath(), e);
        }
    }

    private void copyReportFiles() {
        for (String textAsset : TEXT_ASSETS) {
            InputStream textAssetStream = this.getClass().getClassLoader().getResourceAsStream(textAsset);
            writeBytes(textAssetStream, reportFileOutputStream(textAsset));
        }
    }

    private void writeBytes(InputStream in, OutputStream out) {
        try {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new CucumberException("Cannot copy streams", e);
        }
    }

    final class Summary {
        private int scenarios;
        private int features;

        private int passedSteps;
        private int failedSteps;

        private int passedScenarios;
        private int failedScenarios;

        private long duration;

        public int getScenarios() {
            return scenarios;
        }

        public void setScenarios(int scenarios) {
            this.scenarios = scenarios;
        }

        public int getFeatures() {
            return features;
        }

        public void setFeatures(int features) {
            this.features = features;
        }

        public int getPassedScenarios() {
            return passedScenarios;
        }

        public void setPassedScenarios(int passedScenarios) {
            this.passedScenarios = passedScenarios;
        }

        public int getFailedScenarios() {
            return failedScenarios;
        }

        public void setFailedScenarios(int failedScenarios) {
            this.failedScenarios = failedScenarios;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getPassedSteps() {
            return passedSteps;
        }

        public void setPassedSteps(int passedSteps) {
            this.passedSteps = passedSteps;
        }

        public int getFailedSteps() {
            return failedSteps;
        }

        public void setFailedSteps(int failedSteps) {
            this.failedSteps = failedSteps;
        }
    }

}

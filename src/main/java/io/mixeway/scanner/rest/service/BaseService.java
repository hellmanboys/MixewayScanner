/*
 * @created  2020-08-18 : 23:34
 * @project  MixewayScanner
 * @author   siewer
 */
package io.mixeway.scanner.rest.service;

import io.mixeway.scanner.factory.ScannerFactory;
import io.mixeway.scanner.integrations.ScannerIntegrationFactory;
import io.mixeway.scanner.integrations.scanner.DependencyTrack;
import io.mixeway.scanner.rest.model.ScanRequest;
import io.mixeway.scanner.rest.model.Status;
import io.mixeway.scanner.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BaseService {
    private final static Logger log = LoggerFactory.getLogger(BaseService.class);
    ScannerFactory scannerFactory;
    GitOperations gitOperations;

    public BaseService(ScannerFactory scannerFactory, GitOperations gitOperations){
        this.scannerFactory = scannerFactory;
        this.gitOperations = gitOperations;
    }

    /**
     * Running scan for given request
     *
     * @param scanRequest sent by client
     * @return status
     */
    public ResponseEntity<List<Vulnerability>> runScan(ScanRequest scanRequest) {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        try {
            if (scanRequest.getType().equals(ScannerType.SAST)) {
                if (gitOperations.isProjectPresent(scanRequest)) {
                    GitResponse gitResponse = gitOperations.pull(scanRequest);
                } else {
                    GitResponse gitResponse = gitOperations.clone(scanRequest);
                }

                SourceProjectType sourceProjectType = CodeHelper.getSourceProjectTypeFromDirectory(scanRequest, false);
                if (sourceProjectType == null) {
                    log.error("Repository doesnt contain any of the known types of projects. Current version support only JAVA-Maven projects.");
                    return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
                }

                ScannerIntegrationFactory openSourceScan = scannerFactory.getProperScanner(ScannerPluginType.DEPENDENCYTRACK);
                vulnerabilities.addAll(openSourceScan.runScan(scanRequest));

                switch (sourceProjectType) {
                    case MAVEN:
                        ScannerIntegrationFactory spotbug = scannerFactory.getProperScanner(ScannerPluginType.SPOTBUG);
                        vulnerabilities.addAll(spotbug.runScan(scanRequest));
                        break;
                    case PIP:
                        ScannerIntegrationFactory bandit = scannerFactory.getProperScanner(ScannerPluginType.BANDIT);
                        vulnerabilities.addAll(bandit.runScan(scanRequest));
                        break;
                    case PHP:
                        ScannerIntegrationFactory progpilot = scannerFactory.getProperScanner(ScannerPluginType.PROGPILOT);
                        vulnerabilities.addAll(progpilot.runScan(scanRequest));
                        break;
                    default:
                        log.error("Source Code Language not supported");
                        return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
                }
                // TODO get SAST SCAN
            } else if (scanRequest.getType().equals(ScannerType.DAST)) {
                // TODO run DAST SCAN
            } else {
                log.error("[REST] Got request with unknown scan type {}", scanRequest.getType().toString());
            }
        } catch (Exception e){
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
        return new ResponseEntity<>(vulnerabilities,HttpStatus.OK);
    }
}

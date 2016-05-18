// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.jira.datum;

import java.io.IOException;

import org.talend.components.jira.testutils.Utils;

/**
 * Provides different Json strings for Entities Unit-tests
 */
public class JsonDataProvider {

    /**
     * JSON, which contains total property
     */
    private static String paginationJson;

    /**
     * JSON, which doesn't contain total property
     */
    private static String noPaginationJson;

    /**
     * JSON, which represents project JSON representation
     */
    private static String projectJson;

    /**
     * Provides JSON string, which contains total property
     * 
     * @return JSON string
     * @throws IOException in case of I/O exception
     */
    static String getPaginationJson() {
        if (paginationJson == null) {
            paginationJson = Utils.readFile("src/test/resources/org/talend/components/jira/datum/entities.json");
        }
        return paginationJson;
    }

    /**
     * Provides JSON string, which doesn't contains total property
     * 
     * @return JSON string
     * @throws IOException in case of I/O exception
     */
    static String getNoPaginationJson() {
        if (noPaginationJson == null) {
            noPaginationJson = Utils.readFile("src/test/resources/org/talend/components/jira/datum/noPagination.json");
        }
        return noPaginationJson;
    }

    /**
     * Provides JSON string, which represents project JSON representation
     * 
     * @return JSON string
     * @throws IOException in case of I/O exception
     */
    static String getProjectJson() {
        if (projectJson == null) {
            projectJson = Utils.readFile("src/test/resources/org/talend/components/jira/datum/project.json");
        }
        return projectJson;
    }

}

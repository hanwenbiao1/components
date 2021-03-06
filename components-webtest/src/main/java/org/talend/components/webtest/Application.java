// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.webtest;

import org.apache.cxf.jaxrs.spring.SpringComponentScanServer;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.talend.daikon.spring.BndToSpringBeanNameGenerator;
import org.talend.jsonio.jaxrs.JsonIoProvider;

/**
 * Used the test the component service and the Salesforce components with an external web service.
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.talend.components", nameGenerator = BndToSpringBeanNameGenerator.class, includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = aQute.bnd.annotation.component.Component.class) , excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Osgi") )
@Import(SpringComponentScanServer.class)
public class Application {

    @Value("${service.basepath}")
    private String serviceBasePath;

    @Bean
    public JsonIoProvider jsonProvider(ApplicationContext context) {
        return new JsonIoProvider();
    }

    @Bean
    @DependsOn("jsonProvider")
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        return new ServletRegistrationBean(new CXFServlet(), "/" + serviceBasePath + "/*");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

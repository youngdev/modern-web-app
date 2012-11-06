package com.github.jknack.mwa.solr;

import static org.junit.Assert.assertNotNull;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SolrModuleTest {

  @Test
  public void bootSolr() {
    System.setProperty("solr.home", "/solr-home");

    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(SolrModule.class);

    SolrServer server1 = context.getBean("core1", SolrServer.class);
    assertNotNull(server1);

    SolrServer server2 = context.getBean("core2", SolrServer.class);
    assertNotNull(server2);

    context.destroy();
  }
}
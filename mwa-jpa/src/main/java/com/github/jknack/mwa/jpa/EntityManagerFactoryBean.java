package com.github.jknack.mwa.jpa;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.Collection;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Extends {@link LocalContainerEntityManagerFactoryBean} by registering Hibernate event listeners.
 * Event listeners must be registered in the Spring Application Context.
 *
 * @author edgar.espina
 * @since 0.3.6
 */
public class EntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean {

  /**
   * The application's context. Required.
   */
  private ApplicationContext applicationContext;

  /**
   * Creates a new {@link EntityManagerFactoryBean}.
   *
   * @param applicationContext The application's context. Required.
   */
  public EntityManagerFactoryBean(final ApplicationContext applicationContext) {
    this.applicationContext = notNull(applicationContext, "The application's context is required.");
  }

  @Override
  protected void postProcessEntityManagerFactory(final EntityManagerFactory emf,
      final PersistenceUnitInfo pui) {
    configure((HibernateEntityManagerFactory) emf);
  }

  /**
   * Configure a {@link HibernateEntityManagerFactory}.
   *
   * @param emf The {@link HibernateEntityManagerFactory}.
   */
  protected void configure(final HibernateEntityManagerFactory emf) {
    configure(emf, (SessionFactoryImplementor) emf.getSessionFactory());
  }

  /**
   * Configure {@link SessionFactoryImplementor}.
   *
   * @param emf The entity manager factory.
   * @param sessionFactory The {@link SessionFactoryImplementor}.
   */
  protected void configure(final HibernateEntityManagerFactory emf,
      final SessionFactoryImplementor sessionFactory) {
    // load fixtures
    Map<String, ClassMetadata> metadata = sessionFactory.getAllClassMetadata();
    Environment env = applicationContext.getEnvironment();
    String baseDir = env.getProperty(JpaModule.DB_FIXTURES, JpaModule.DB_DEFAULT_FIXTURES);
    notEmpty(baseDir, "{} isn't set", JpaModule.DB_FIXTURES);
    JpaFixtures.load(applicationContext, emf, baseDir, metadata);

    // configure
    configure(sessionFactory.getServiceRegistry());
  }

  /**
   * Configure the {@link ServiceRegistryImplementor}.
   *
   * @param registry The {@link ServiceRegistryImplementor}.
   */
  protected void configure(final ServiceRegistryImplementor registry) {
    configure(registry.getService(EventListenerRegistry.class));
  }

  /**
   * Looks for hibernate event listeners and register all them.
   *
   * @param registry The event listener registry.
   */
  @SuppressWarnings({"unchecked", "rawtypes" })
  private void configure(final EventListenerRegistry registry) {
    Collection<EventType> values = EventType.values();
    for (EventType eventType : values) {
      EventListenerGroup group = registry.getEventListenerGroup(eventType);
      Collection listeners = applicationContext.getBeansOfType(eventType.baseListenerInterface())
          .values();
      for (Object listener : listeners) {
        group.appendListener(listener);
      }
    }
  }
}

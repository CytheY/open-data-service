package org.jvalue.ods.main;


import com.google.inject.Guice;
import com.google.inject.Injector;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.GenericConstraintDef;
import org.jvalue.ods.admin.rest.AdminFilterChainApi;
import org.jvalue.ods.api.processors.ProcessorReferenceChainDescription;
import org.jvalue.ods.data.DataModule;
import org.jvalue.ods.data.DataSourceManager;
import org.jvalue.ods.db.DbModule;
import org.jvalue.ods.monitoring.DbHealthCheck;
import org.jvalue.ods.monitoring.MonitoringModule;
import org.jvalue.ods.notifications.NotificationsModule;
import org.jvalue.ods.processor.ProcessorModule;
import org.jvalue.ods.processor.reference.ValidChainReference;
import org.jvalue.ods.rest.DataApi;
import org.jvalue.ods.rest.DataSourceApi;
import org.jvalue.ods.rest.DataViewApi;
import org.jvalue.ods.rest.DbExceptionMapper;
import org.jvalue.ods.rest.FilterChainApi;
import org.jvalue.ods.rest.FilterDescriptionApi;
import org.jvalue.ods.rest.JsonExceptionMapper;
import org.jvalue.ods.rest.JsonMixins;
import org.jvalue.ods.rest.NotificationClientRegistrationApi;
import org.jvalue.ods.rest.PluginApi;
import org.jvalue.ods.rest.RestModule;
import org.jvalue.ods.utils.GuiceConstraintValidatorFactory;

import java.util.Map;

import javax.validation.Validation;
import javax.ws.rs.core.Context;

import io.dropwizard.Application;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public final class OdsApplication extends Application<OdsConfig> {


	public static void main(String[] args) throws Exception {
		new OdsApplication().run(args);
	}


	@Override
	public String getName() {
		return "Open Data Service";
	}


	@Override
	public void initialize(Bootstrap<OdsConfig>configuration) {
		JsonMixins mixins = new JsonMixins();
		for (Map.Entry<Class<?>, Class<?>> mixinPair : mixins.getMixins().entrySet()) {
			configuration.getObjectMapper().addMixInAnnotations(mixinPair.getKey(), mixinPair.getValue());
		}
	}


	@Override
	@Context
	public void run(OdsConfig configuration, Environment environment) {
		Injector injector = Guice.createInjector(
				new MonitoringModule(environment.metrics()),
				new ConfigModule(configuration),
				new ProcessorModule(),
				new RestModule(),
				new DbModule(configuration.getCouchdbUrl()),
				new NotificationsModule(),
				new DataModule());

		// start data grabbing
		environment.lifecycle().manage(injector.getInstance(DataSourceManager.class));
		environment.jersey().getResourceConfig().register(MultiPartFeature.class);
		environment.jersey().register(injector.getInstance(DataSourceApi.class));
		environment.jersey().register(injector.getInstance(DataApi.class));
		environment.jersey().register(injector.getInstance(FilterChainApi.class));
		environment.jersey().register(injector.getInstance(DataViewApi.class));
		environment.jersey().register(injector.getInstance(NotificationClientRegistrationApi.class));
		environment.jersey().register(injector.getInstance(PluginApi.class));
		environment.jersey().register(injector.getInstance(FilterDescriptionApi.class));
		environment.jersey().register(new DbExceptionMapper());
		environment.jersey().register(new JsonExceptionMapper());

		// configure administration
		environment.healthChecks().register(DbHealthCheck.class.getSimpleName(), injector.getInstance(DbHealthCheck.class));
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(environment.metrics());
		JerseyContainerHolder jerseyContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));
		jerseyConfig.register(injector.getInstance(AdminFilterChainApi.class));
		environment.admin().addServlet("admin resources", jerseyContainerHolder.getContainer()).addMapping("/admin/*");

		// setup validation for external classes
		HibernateValidatorConfiguration validatorContext = Validation.byProvider(HibernateValidator.class).configure();
		ConstraintMapping mapping = validatorContext.createConstraintMapping();
		mapping.type(ProcessorReferenceChainDescription.class).constraint(new GenericConstraintDef<>(ValidChainReference.class));

		// setup Guice DI for hibernate validator
		environment.setValidator(validatorContext.addMapping(mapping)
				.constraintValidatorFactory(new GuiceConstraintValidatorFactory(injector))
				.buildValidatorFactory()
				.getValidator());
	}

}

package com.mudah.auto.cars.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@Configuration
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${mudah.dbconfig}")
    private String dbConfig;

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(r2dbcUrl);
    }

    @Bean
    public ConnectionFactoryInitializer databaseInitializer(ConnectionFactory connectionFactory) {
        var initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        var databasePopulator = new CompositeDatabasePopulator();

        if ("create".equalsIgnoreCase(dbConfig)) {
            databasePopulator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("DropTables/DropTables.sql")));
            databasePopulator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("Tables/ActionToken.sql")));
        } else if ("update".equalsIgnoreCase(dbConfig)) {
            databasePopulator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("UpdateTables/UpdateTables.sql")));
        }

        initializer.setDatabasePopulator(databasePopulator);
        return initializer;
    }
}

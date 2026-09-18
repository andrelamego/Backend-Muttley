package com.fatec.muttley.config;

import org.hibernate.community.dialect.MariaDBLegacyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

/** Mantém compatibilidade com o MariaDB 10.4 local sem forçar seu dialeto em outros bancos. */
public class DialetoBancoResolver implements DialectResolver {
    @Override
    public Dialect resolveDialect(DialectResolutionInfo info) {
        boolean mariaDb = "MariaDB".equals(info.getDatabaseName())
                || (info.getDriverName() != null && info.getDriverName().startsWith("MariaDB"));
        if (mariaDb && (info.getDatabaseMajorVersion() < 10
                || (info.getDatabaseMajorVersion() == 10 && info.getDatabaseMinorVersion() < 6))) {
            return new MariaDBLegacyDialect(info);
        }
        return null;
    }
}

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.AbstractDatabaseComponent;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 * Oracle Flash Recovery Area Component.
 * 
 * @author Richard Hensman
 */
public class OracleFlashRecoveryAreaComponent extends AbstractDatabaseComponent implements MeasurementFacet {

    private static final String SQL_AVAILABLE = "SELECT COUNT(*) FROM v$recovery_file_dest WHERE name = ?";
    private static final String SQL_VALUES =
        "SELECT space_limit spaceLimit, space_used spaceUsed, space_reclaimable spaceReclaimable, number_of_files numberOfFiles, (space_used/space_limit) usedPercent " +
        "FROM v$recovery_file_dest WHERE name = ?";


    private static Log log = LogFactory.getLog(OracleFlashRecoveryAreaComponent.class);

    public AvailabilityType getAvailability() {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = getConnection().prepareStatement(SQL_AVAILABLE);
            statement.setString(1, this.resourceContext.getResourceKey());
            resultSet = statement.executeQuery();
            if (resultSet.next() && (resultSet.getInt(1) == 1)) {
                return AvailabilityType.UP;
            }
        } catch (SQLException e) {
            log.debug("unable to query", e);
        } finally {
            JDBCUtil.safeClose(statement, resultSet);
        }

        return AvailabilityType.DOWN;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        Map<String, Double> values = DatabaseQueryUtility.getNumericQueryValues(this, SQL_VALUES,
                this.resourceContext.getResourceKey());
        for (MeasurementScheduleRequest request : metrics) {
            Double d = values.get(request.getName().toUpperCase(Locale.US));
            if (d != null) {
                report.addData(new MeasurementDataNumeric(request, d));
            }
        }
    }
}
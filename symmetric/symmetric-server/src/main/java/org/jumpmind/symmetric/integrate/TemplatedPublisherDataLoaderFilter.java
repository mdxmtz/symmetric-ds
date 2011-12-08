/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  */

package org.jumpmind.symmetric.integrate;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.symmetric.load.IDataLoaderContext;
import org.jumpmind.symmetric.load.IDataLoaderFilter;

/**
 * A convenience class that allows the end user to template a message using
 * SymmetricDS filter data.
 * </p>
 * You may use %COLUMN% formatted tokens in your template data which will be
 * replaced by data coming in through the filter. The following tokens are also
 * supported:
 * <ol>
 * <li>%DMLTYPE% - evaluates to INSERT, UPDATE or DELETE</li>
 * <li>%TIMESTAMP% - evaluates to ms value returned by
 * System.currentTimeInMillis()</li>
 * </ol>
 * </p>
 * If you have special formatting needs, implement the {@link IFormat} interface
 * and map your formatter to the column you want to 'massage.'
 *
 * 
 */
public class TemplatedPublisherDataLoaderFilter extends AbstractTextPublisherDataLoaderFilter {

    static final Log logger = LogFactory.getLog(TemplatedPublisherDataLoaderFilter.class);

    private String headerTableTemplate;
    private String footerTableTemplate;
    private String contentTableTemplate;
    private Map<String, IFormat> columnNameToDataFormatter;
    private boolean processDelete = true;
    private boolean processInsert = true;
    private boolean processUpdate = true;

    private IDataLoaderFilter dataFilter;

    @Override
    protected String addTextElementForDelete(IDataLoaderContext ctx, String[] keys) {
        if (this.dataFilter == null || this.dataFilter.filterDelete(ctx, keys)) {
            String template = null;
            if (processDelete) {
                template = contentTableTemplate;
                if (template != null) {
                    template = fillOutTemplate(DmlType.DELETE, template, ctx, null, keys);
                }
            }
            return template;
        } else {
            return null;
        }
    }

    @Override
    protected String addTextElementForInsert(IDataLoaderContext ctx, String[] data) {
        if (this.dataFilter == null || this.dataFilter.filterInsert(ctx, data)) {
            String template = null;
            if (processInsert) {
                template = contentTableTemplate;
                if (template != null) {
                    template = fillOutTemplate(DmlType.INSERT, template, ctx, data, null);
                }
            }
            return template;
        } else {
            return null;
        }
    }

    @Override
    protected String addTextElementForUpdate(IDataLoaderContext ctx, String[] data, String[] keys) {
        if (this.dataFilter == null || this.dataFilter.filterUpdate(ctx, data, keys)) {
            String template = null;
            if (processUpdate) {
                template = contentTableTemplate;
                if (template != null) {
                    template = fillOutTemplate(DmlType.UPDATE, template, ctx, data, keys);
                }
            }
            return template;
        } else {
            return null;
        }
    }

    @Override
    protected String addTextFooter(IDataLoaderContext ctx) {
        return footerTableTemplate;
    }

    @Override
    protected String addTextHeader(IDataLoaderContext ctx) {
        return headerTableTemplate;
    }

    protected String fillOutTemplate(DmlType dmlType, String template, IDataLoaderContext ctx, String[] data,
            String[] keys) {
        String[] colNames = null;

        if (data == null) {
            colNames = ctx.getKeyNames();
            data = keys;
        } else {
            colNames = ctx.getColumnNames();
        }

        for (int i = 0; i < data.length; i++) {
            String col = colNames[i];
            template = replace(template, col, format(col, data[i]));
        }

        template = template.replace("DMLTYPE", dmlType.name());
        template = template.replace("TIMESTAMP", Long.toString(System.currentTimeMillis()));

        return template;
    }

    protected String format(String col, String data) {
        if (columnNameToDataFormatter != null) {
            IFormat formatter = columnNameToDataFormatter.get(col);
            if (formatter != null) {
                try {
                    data = formatter.format(data);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return data;
    }

    protected String replace(String template, String token, String value) {
        if (value == null) {
            value = "";
        }
        
        if (template != null) {
            template = template.replace("%" + token + "%", value);
        }
        
        return template;
    }

    public void setColumnNameToDataFormatter(Map<String, IFormat> columnNameToDataFormatter) {
        this.columnNameToDataFormatter = columnNameToDataFormatter;
    }

    public void setProcessDelete(boolean processDeletes) {
        this.processDelete = processDeletes;
    }

    public void setProcessInsert(boolean processInserts) {
        this.processInsert = processInserts;
    }

    public void setProcessUpdate(boolean processUpdates) {
        this.processUpdate = processUpdates;
    }

    public void setHeaderTableTemplate(String headerTableTemplate) {
        this.headerTableTemplate = headerTableTemplate;
    }

    public void setFooterTableTemplate(String footerTableTemplate) {
        this.footerTableTemplate = footerTableTemplate;
    }

    public void setContentTableTemplate(String contentTableTemplate) {
        this.contentTableTemplate = contentTableTemplate;
    }

    public interface IFormat {
        public String format(String data) throws ParseException;
    }

    public void setDataFilter(IDataLoaderFilter dataFilter) {
        this.dataFilter = dataFilter;
    }

}
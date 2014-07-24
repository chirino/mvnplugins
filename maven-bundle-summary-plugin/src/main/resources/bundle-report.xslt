<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fbr="http://fabric8.io/bundle-report">

    <xsl:output doctype-public="html" encoding="UTF-8" method="html" />

    <xsl:template match="/">
        <html>
            <head>
                <meta charset="utf-8"/>
                <style type="text/css">
                    body { font-family: sans-serif; margin: 1em 7em }
                    h1 { border-bottom: 1px solid gray; margin: 2em 0 0.5em 0 }
                    h1.errors { border-bottom: 1px solid red; color: #800 }
                    div.bundle { border: 1px solid #ddd; padding: 0; margin: 5px 0 }
                    div.bundle-id { background-color: #248; color: white; font-weight: bold; padding: 10px; font-size: 12pt }
                    div.package-id { background-color: #68c; color: white; font-weight: bold; padding: 5px; font-size: 10pt }
                    div.version { padding: 5px }
                    div.version-id { font-weight: bold; padding: 2px; font-size: 8pt; border-bottom: 1px solid #68c }
                    div.exports-header { padding: 5px; font-weight: bold; color: white; background-color: green }
                    div.imports-header { padding: 5px; font-weight: bold; color: white; background-color: red }
                    table.exports-imports { width: 100%; border-collapse: collapse }
                    table.exports-imports td, table.exports-imports th { border: 1px solid gray; padding: 3px }
                    ul { padding: 0; margin: 0 5px }
                    li { list-style: inside; margin-left: 5px; padding: 2px }
                    div.help { font-style: italic; color: gray }
                </style>
            </head>
            <body>
                <h1>Bundle summary report</h1>
                <xsl:apply-templates select="/fbr:bundle-report/fbr:bundles" />
                <h1>Exports report</h1>
                <div class="help">(List of exported packages. No package should be exported by more than one bundle. No package should be exported in more than one version.)</div>
                <xsl:for-each select="/fbr:bundle-report/fbr:exports/fbr:export">
                    <xsl:call-template name="package">
                        <xsl:with-param name="bundles-title" select="'Exporting bundles'" />
                    </xsl:call-template>
                </xsl:for-each>
                <h1>Imports report</h1>
                <div class="help">(List of imported packages. Each package should be imported with exact version or version range to minimize conflicts.)</div>
                <xsl:for-each select="/fbr:bundle-report/fbr:imports/fbr:import">
                    <xsl:call-template name="package">
                        <xsl:with-param name="bundles-title" select="'Importing bundles'" />
                    </xsl:call-template>
                </xsl:for-each>
                <h1 class="errors">Bundle problems report</h1>
                <h1>Duplicate Exports report</h1>
                <xsl:for-each select="/fbr:bundle-report/fbr:export-conflicts/fbr:export">
                    <xsl:call-template name="package">
                        <xsl:with-param name="bundles-title" select="'Exporting bundles'" />
                    </xsl:call-template>
                </xsl:for-each>
                <h1>Conflicting Imports report</h1>
                <xsl:for-each select="/fbr:bundle-report/fbr:import-conflicts/fbr:import">
                    <xsl:call-template name="package">
                        <xsl:with-param name="bundles-title" select="'Importing bundles'" />
                    </xsl:call-template>
                </xsl:for-each>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="fbr:bundle">
        <div class="bundle">
            <div class="bundle-id"><xsl:value-of select="@symbolic-name" /> &#183; <xsl:value-of select="@version" /></div>
            <table style="width: 100%">
                <col span="2" width="50%" />
                <tr>
                    <td style="vertical-align: top">
                        <div class="exports-header">exports</div>
                        <table class="exports-imports">
                            <col span="2" width="50%" />
                            <thead>
                                <tr>
                                    <th>package</th>
                                    <th>version (range)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <xsl:for-each select="fbr:exports/fbr:export">
                                    <xsl:call-template name="import-export" />
                                </xsl:for-each>
                            </tbody>
                        </table>
                    </td>
                    <td style="vertical-align: top">
                        <div class="imports-header">imports</div>
                        <table class="exports-imports">
                            <col span="2" width="50%" />
                            <thead>
                                <tr>
                                    <th>package</th>
                                    <th>version (range)</th>
                                </tr>
                            </thead>
                            <tbody>
                                <xsl:for-each select="fbr:imports/fbr:import">
                                    <xsl:call-template name="import-export" />
                                </xsl:for-each>
                            </tbody>
                        </table>
                    </td>
                </tr>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="package">
        <xsl:param name="bundles-title" select="'bundles'" />
        <div class="bundle">
            <div class="package-id"><xsl:value-of select="@package" /></div>
            <div style="padding: 5px; font-weight: bold"><xsl:value-of select="$bundles-title" /></div>
            <xsl:for-each select="fbr:version">
                <div class="version">
                    <div class="version-id"><xsl:value-of select="@version" /></div>
                    <ul>
                        <xsl:for-each select="fbr:by-bundle">
                            <li><xsl:value-of select="@symbolic-name" />:<xsl:value-of select="@version" /></li>
                        </xsl:for-each>
                    </ul>
                </div>
            </xsl:for-each>
        </div>
    </xsl:template>

    <xsl:template name="import-export">
        <tr>
            <td><xsl:value-of select="@package" /></td>
            <td><xsl:value-of select="@version" /></td>
        </tr>
    </xsl:template>

</xsl:stylesheet>

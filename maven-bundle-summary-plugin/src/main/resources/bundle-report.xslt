<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fbr="http://fabric8.io/bundle-report">

    <xsl:output doctype-public="html" encoding="UTF-8" method="html" />

    <xsl:template match="/">
        <html>
            <head>
                <meta charset="utf-8" />
                <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" />
                <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css" />
                <style type="text/css">
                    body { position: relative }
                    h1 { margin: 2em 0 0.5em 0 }
                    div.bundle { border: 1px solid #ddd; padding: 0; margin: 5px 0 }
                    div.bundle-id { background-color: #248; color: white; font-weight: bold; padding: 10px; font-size: 12pt }
                    div.package-id { background-color: #68c; color: white; font-weight: bold; padding: 5px; font-size: 10pt }
                    div.version { padding: 5px }
                    div.version-id { font-weight: bold; padding: 2px; font-size: 8pt; border-bottom: 1px solid #68c }
                    div.exports-header { padding: 5px; font-weight: bold; color: white; background-color: green }
                    div.imports-header { padding: 5px; font-weight: bold; color: white; background-color: red }
                    div.help { font-style: italic; color: gray }
                    a.error { color: red !important; text-shadow: 0 0 12px white !important }
                    p { padding: 10px }
                </style>
                <script src="http://code.jquery.com/jquery-2.1.1.js"></script>
                <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
            </head>
            <body data-spy="scroll" data-target="#n">
                <div id="n">
                    <nav id="nav" class="navbar navbar-inverse navbar-fixed-top" role="navigation">
                        <div class="container">
                            <div class="navbar-collapse collapse">
                                <ul class="nav navbar-nav" role="tablist">
                                    <li class="active">
                                        <a href="#report">Bundle Summary (<xsl:value-of select="count(/fbr:bundle-report/fbr:bundles/fbr:bundle)" />)
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#exports">Exports (<xsl:value-of select="count(/fbr:bundle-report/fbr:exports/fbr:export)" />)
                                        </a>
                                    </li>
                                    <li>
                                        <a href="#imports">Imports (<xsl:value-of select="count(/fbr:bundle-report/fbr:imports/fbr:import)" />)
                                        </a>
                                    </li>
                                    <li>
                                        <a class="error" href="#exports-errors">Exports-Errors (<xsl:value-of select="count(/fbr:bundle-report/fbr:export-conflicts/fbr:export)" />)
                                        </a>
                                    </li>
                                    <li>
                                        <a class="error" href="#imports-errors">Imports-Errors (<xsl:value-of select="count(/fbr:bundle-report/fbr:import-conflicts/fbr:import)" />)
                                        </a>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </nav>
                </div>

                <div class="container-fluid">
                    <a id="report" />
                    <h1>Bundle summary report</h1>
                    <xsl:apply-templates select="/fbr:bundle-report/fbr:bundles" />
                    <a id="exports" />
                    <h1>Exports report</h1>
                    <div class="help">(List of exported packages. No package should be exported by more than one bundle.
                        No package should be exported in more than one version.)
                    </div>
                    <xsl:for-each select="/fbr:bundle-report/fbr:exports/fbr:export">
                        <xsl:call-template name="package">
                            <xsl:with-param name="bundles-title" select="'Exporting bundles'" />
                        </xsl:call-template>
                    </xsl:for-each>
                    <a id="imports" />
                    <h1>Imports report</h1>
                    <div class="help">(List of imported packages. Each package should be imported with exact version or
                        version range to minimize conflicts.)
                    </div>
                    <xsl:for-each select="/fbr:bundle-report/fbr:imports/fbr:import">
                        <xsl:call-template name="package">
                            <xsl:with-param name="bundles-title" select="'Importing bundles'" />
                        </xsl:call-template>
                    </xsl:for-each>
                    <a id="exports-errors" />
                    <h1 class="text-danger">Duplicate Exports report</h1>
                    <xsl:for-each select="/fbr:bundle-report/fbr:export-conflicts/fbr:export">
                        <xsl:call-template name="package">
                            <xsl:with-param name="bundles-title" select="'Exporting bundles'" />
                        </xsl:call-template>
                    </xsl:for-each>
                    <a id="imports-errors" />
                    <h1 class="text-danger">Conflicting Imports report</h1>
                    <xsl:for-each select="/fbr:bundle-report/fbr:import-conflicts/fbr:import">
                        <xsl:call-template name="package">
                            <xsl:with-param name="bundles-title" select="'Importing bundles'" />
                        </xsl:call-template>
                    </xsl:for-each>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="fbr:bundle">
        <div class="bundle">
            <p class="bg-primary">
                <xsl:value-of select="@symbolic-name" /> &#183;
                <xsl:value-of select="@version" />
            </p>
            <table class="table table-condensed">
                <col span="3" width="33%" />
                <tbody>
                    <tr>
                        <td style="vertical-align: top">
                            <p class="bg-info">exports</p>
                            <table class="table table-condensed">
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
                            <p class="bg-info">imports</p>
                            <table class="table table-condensed">
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
                        <td style="vertical-align: top">
                            <p class="bg-info">private packages</p>
                            <table class="table table-condensed">
                                <col span="2" width="50%" />
                                <thead>
                                    <tr>
                                        <th>package</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="fbr:privates/fbr:private">
                                        <tr>
                                            <td>
                                                <xsl:value-of select="@package" />
                                            </td>
                                        </tr>
                                    </xsl:for-each>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <!--tr>
                        <td style="vertical-align: top">
                            <p class="bg-info">required capabilities</p>
                            <table class="table table-condensed">
                                <col span="2" width="50%" />
                                <thead>
                                    <tr>
                                        <th>name</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="fbr:required-capabilities/fbr:capability">
                                        <tr>
                                            <td>
                                                <xsl:value-of select="@name" />
                                            </td>
                                        </tr>
                                    </xsl:for-each>
                                </tbody>
                            </table>
                        </td>
                        <td style="vertical-align: top">
                            <p class="bg-info">provided capabilities</p>
                            <table class="table table-condensed">
                                <col span="2" width="50%" />
                                <thead>
                                    <tr>
                                        <th>name</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="fbr:provided-capabilities/fbr:capability">
                                        <tr>
                                            <td>
                                                <xsl:value-of select="@name" />
                                            </td>
                                        </tr>
                                    </xsl:for-each>
                                </tbody>
                            </table>
                        </td>
                        <td></td>
                    </tr-->
                </tbody>
            </table>
        </div>
    </xsl:template>

    <xsl:template name="package">
        <xsl:param name="bundles-title" select="'bundles'" />
        <div class="bundle">
            <p class="bg-primary">
                <xsl:value-of select="@package" />
            </p>
            <div class="text-info" style="padding: 2px 10px">
                <xsl:value-of select="$bundles-title" />
            </div>
            <xsl:for-each select="fbr:version">
                <div class="version">
                    <div class="version-id">
                        <xsl:value-of select="@version" />
                    </div>
                    <ul>
                        <xsl:for-each select="fbr:by-bundle">
                            <li><xsl:value-of select="@symbolic-name" />:<xsl:value-of select="@version" />
                            </li>
                        </xsl:for-each>
                    </ul>
                </div>
            </xsl:for-each>
        </div>
    </xsl:template>

    <xsl:template name="import-export">
        <tr>
            <td>
                <xsl:value-of select="@package" />
            </td>
            <td>
                <xsl:value-of select="@version" />
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>

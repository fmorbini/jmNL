<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/log">
  <html>
		<style>
			user { font-size: 13px; }
			userin { font-size: 24px; }
		</style>
  		<script type="text/javascript">
		
		function exp_coll(ind) {
			s = document.getElementById(ind);
			
			if (s.style.display == 'none') {
				s.style.display = 'block';
			} else if (s.style.display == 'block') {
				s.style.display = 'none';
			}
		}
		
		</script>

  <body>
  <h2>Log <xsl:value-of select="@id"/></h2>
  <table border="1">
    <tr bgcolor="#9acd32">
      <th>Seq</th>
      <th>[s]</th>
      <th>Text</th>
    </tr>
	<xsl:for-each select=".">
		<xsl:for-each select="user|system|state|other">
			<xsl:choose>
			<xsl:when test="name(.)='user'">
				<xsl:call-template name="user"/>
			</xsl:when>
			<xsl:when test="name(.)='state'">
				<xsl:call-template name="state"/>
			</xsl:when>
			<xsl:when test="name(.)='system'">
				<xsl:call-template name="system"/>
			</xsl:when>
			<xsl:when test="name(.)='other'">
				<xsl:call-template name="other"/>
			</xsl:when>
			<xsl:otherwise/>
			</xsl:choose>
		</xsl:for-each>
	</xsl:for-each>

  </table>
  </body>
  </html>
</xsl:template>

<xsl:template match="user" name="user">
<tr>
	<td><xsl:value-of select="@number"/></td>
	<td><xsl:value-of select="@delta"/></td>
	<td>
		<xsl:variable name="spanid" select="generate-id(.)"/>
		<a href="javascript:exp_coll('{$spanid}');"><span  style="font-size: 24px;"><xsl:value-of select="text"/></span></a>
		<span id="{$spanid}" style="display:none;">
			<div style="font-size: 12px;">
			<table>
			<xsl:for-each select="./nlu">
				<tr><td><xsl:value-of select="@id"/></td><td><xsl:value-of select="@prob"/></td></tr>
				<tr><td colspan="2"><xsl:value-of select="@payload"/></td></tr>
			</xsl:for-each>
			</table>
			</div>
		</span>
		<!-- <xsl:variable name="index" select="$index + 1"/> -->
	</td>
</tr>
</xsl:template>

<xsl:template match="system" name="system">
<tr>
	<td><xsl:value-of select="@number"/></td>
	<td><xsl:value-of select="@delta"/></td>
	<td>
		<xsl:variable name="spanid" select="generate-id(.)"/>
		<a href="javascript:exp_coll('{$spanid}');"><span  style="font-size: 20px; color: #000000"><xsl:value-of select="text"/></span></a>
		<span id="{$spanid}" style="display:none;">
			<font size="1">
			<table>
				<tr><td><xsl:value-of select="@id"/></td></tr>
			</table>
			</font>
		</span>
	</td>
</tr>
</xsl:template>

<xsl:template match="other" name="other">
<tr>
	<td><span  style="font-size: 8px; color: #000000"><xsl:value-of select="@number"/></span></td>
	<td><span  style="font-size: 8px; color: #000000"><xsl:value-of select="@delta"/></span></td>
	<td>
		<xsl:variable name="spanid" select="generate-id(.)"/>
		<a href="javascript:exp_coll('{$spanid}');"><span  style="font-size: 8px; color: #000000"><xsl:value-of select="text"/></span></a>
		<span id="{$spanid}" style="display:none;">
			<table>
				<tr><td><xsl:value-of select="@id"/></td></tr>
			</table>
		</span>
	</td>
</tr>
</xsl:template>

<xsl:template match="state" name="state">
	<tr>
		<td colspan="3">
			<xsl:variable name="spanid" select="generate-id(.)"/>
			<a href="javascript:exp_coll('{$spanid}');"><span  style="font-size: 8px; font-color: #000000"><xsl:value-of select="@type"/></span></a>
			<span id="{$spanid}" style="display:none;">
				<div  style="font-size: 10px;">
				<table>
				<xsl:for-each select="./assign">
					<tr><td><xsl:value-of select="@var"/></td><td><xsl:value-of select="@value"/></td></tr>
				</xsl:for-each>
				</table>
				</div>
			</span>
			<!-- <xsl:variable name="index" select="$index + 1"/> -->
		</td>
	</tr>
</xsl:template>
</xsl:stylesheet>
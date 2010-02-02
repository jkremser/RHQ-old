function ArchiveFile(p, c) {
	this.path = p;
	this.dirty = 0;
	this.contents = c;
}

/*
 * This is a class for editing a collection of files, represented by a map.
 * There is a list of links in the left column, each which represent a file, a
 * textarea input that changes based on which link is selected
 */
function MultiFileEdit() {
	this.currentIndex = 0;
	this.archive = [];
	this.originalCopies = [];
	this.disabled = 0;
	this.uploadUrl = "upload.html";
	this.downloadUrl = "#";
	

	this.addArchive = function(p,c){		
		this.originalCopies.push( new ArchiveFile(p,c));
	}

	this.initializeArchive = function() {
		this.archive = [];
		if ( this.originalCopies.length < 1){
			this.archive.push(new ArchiveFile("/dev/null","No files specified"));
		}else {	
			var i;
			for (i=0; i < this.originalCopies.length ;i++){
				var original = this.originalCopies[i];
				this.archive.push(new ArchiveFile(original.path,original.contents));
			}
		}
	}
	
	this.setTextAreaValue = function(id) {
		if (this.currentIndex % 2 == 0) {
			document.getElementById("file-row-" + this.currentIndex).className = "OddRow";
		} else {
			document.getElementById("file-row-" + this.currentIndex).className = "EvenRow";
		}

		this.currentIndex = id;
		document.getElementById("testForm").textarea.value = multiFileEdit.archive[this.currentIndex].contents;
		document.getElementById("file-row-" + id).className = "SelectedRow";
		document.getElementById("testForm").textarea.focus();
		document.getElementById("current-path-span").firstChild.data = multiFileEdit.archive[this.currentIndex].path;
	}

	this.resetArchive = function() {
		this.initializeArchive();
		this.setTextAreaValue(0);
		window.onbeforeunload = 0;
		for (i = 0; i < this.archive.length; i++) {
			document.getElementById("dirty-span-" + i).style.visibility = "hidden";
			document.getElementById("reset-span-" + i).style.visibility = "hidden";			
		}
	}
	
	this.resetFile = function(index){
		 if (index >= this.originalCopies.length) return;
		 if (index <   0  ) return;
		 var original = this.originalCopies[index];
		 this.archive[index] = new ArchiveFile(original.path,original.contents);
		 document.getElementById("dirty-span-" + index).style.visibility = "hidden";
		 document.getElementById("reset-span-" + index).style.visibility = "hidden";			
		 this.setTextAreaValue(index);
	}
	
	function askConfirm() {
		return "You have unsaved changes.";
	}

	this.updateArchive = function() {
		window.onbeforeunload = askConfirm;
		this.archive[this.currentIndex].contents = document
				.getElementById("testForm").textarea.value;
		this.archive[this.currentIndex].dirty = 1;
		document.getElementById("dirty-span-" + this.currentIndex).style.visibility = "visible";
		document.getElementById("reset-span-" + this.currentIndex).style.visibility = "visible";
	}

	this.download= function() {
		var generator = window.open(this.downloadUrl,'');
	}
	
	this.upload = function(){
		alert("upload called");
	}
	
	this.commit = function(){
		alert("commit called");
		return false;
	}
	
	this.drawStyleSheet = function(){
		document.write("<style type=\"text/css\">");
		document.write("td.multi-edit-table {");
		document.write("vertical-align: top;");
		document.write("valign: top;");
		document.write("height: = 300;");
		document.write("}");
		document.write("tr.EvenRow {")
		document.write("background-color:#a4b2b9;");
		document.write("}");
		document.write("tr.OddRow {");
		document.write("background-color:grey;");
		document.write("}");
		document.write("tr.SelectedRow {");
		document.write("background-color:white;");
		document.write("}");
		document.write("span.reset {");
		document.write("background-color:grey; color:white; visibility:hidden; ");		
		document.write("}");
		
		document.write("</style>");	
	}
	
	this.generateLinks = function() {
		var i = 0;
		var style = 0;
		var divclass = "";
		document.write("<table BORDER=0 RULES=NONE FRAME=BOX><th><td>Configuration File Paths</td><td></td></th>");
		for (i = 0; i < multiFileEdit.archive.length; i++) {
			if (i == 0) {
				divclass = "SelectedRow"
			} else if (style != 0) {
				style = 0;
				divclass = "OddRow"
			} else {
				style = 1;
				divclass = "EvenRow"
			}
			document.write("<tr id='file-row-" + i + "'class='" + divclass
					+ "'>");
			document.write("<td>");
			document.write("<span style=\"visibility:hidden; \" > XX </span>");
			document.write("<span id='dirty-span-" + i
					+ "' style=\"visibility:hidden;\" > * </span>");
			document.write("</td><td>");
			document.write("<a href=\"#\"  onclick='multiFileEdit.setTextAreaValue("
							+ i + ")' >" + multiFileEdit.archive[i].path);
			document.write("</a>");
			document.write("<span class='reset' id='reset-span-" + i + "' ");
			document.write(" onmouseup=' multiFileEdit.resetFile("+i+");'>"); 
			document.write(" Undo</span>");
			document.write("</td></tr>");
		}
		document.write("</table>");
	}

	this.drawTable = function() {
		this.initializeArchive();
		this.drawStyleSheet();		
		
		document
				.write("<table class='multi-edit-table' border='0' cellpadding='0' >");
		document.write("<tbody>");
		document.write("<tr>");
		document.write("<td class='multi-edit-table'   bgcolor='#a4b2b9'>");
		this.generateLinks();
		document.write("</td>");
		document.write("<td>");
		document.write("<div><span id='current-path-span'>");
		document.write(multiFileEdit.archive[this.currentIndex].path);
		document.write("</span>  ..................");
		document.write("<a href='#'");
		document.write("onclick=\"multiFileEdit.download()\"> ");
		document.write("<img src='/images/download.png'/> Download</a>");
		document.write("<a href='#' ");
		document.write("onclick=\"multiFileEdit.fullscreen()\"> ");
		document.write("<img src='/images/viewfullscreen.png'/> Full Screen</a>")
		document.write("</div>");
		document.write("<textarea id='textarea' ");
		if (this.disabled) {
			document.write("disabled ");
		}
		document
				.write("onkeyup='multiFileEdit.updateArchive();' cols='80' rows='25'>");
		document.write(multiFileEdit.archive[0].contents);
		document.write("</textarea>");
		if (!this.disabled){
			document.write("<div>upload New Version</div>");
			document.write("<div>Select A File: <input type='file'  onchange='multiFileEdit.upload();' /> </div>");
		}

		document.write("</td>");
		document.write("</tr>");
		
		document.write("<tr>");
		document.write("<td>");
		if (!this.disabled){
			document.write("<input type='submit' value='commit' onclick='multiFileEdit.commit();'/>");
			document.write("<input type='reset' onclick='multiFileEdit.resetArchive();' />");
		}
		document.write("</td>");
		document.write("<td></td>");
		document.write("</tr>");
		
		document.write("</tbody>");
		document.write("</table>");
		
		
		

	}

	this.fullscreen = function() {

		var generator = window.open('', 'full screen',
				'width=1024,height=768,left=0,top=100,screenX=0,screenY=0');
		generator.document.write('<html><head><title>Popup</title>');
		generator.document.write('<link rel="stylesheet" href="style.css">');
		generator.document
				.write('<script language="JavaScript" type="text/javascript" src="/js/multi-file-edit.js"></script>');
		generator.document.write('</head><body>');
		generator.document.write("<form id='full-screen-edit-form'>");
		generator.document.write("<div><span id='current-path-span'>"
				+ multiFileEdit.archive[this.currentIndex].path + "</span> ");
		generator.document
				.write("<a href=\"javascript:self.close();\"  >Close</a>..................");
		generator.document.write("</div>");
		generator.document
				.write("<textarea id='textarea'  onkeyup='update_parent();' ");
		if (this.disabled) {
			generator.document.write("disabled ");
		}
		generator.document.write("cols='200' rows='45'>");
		generator.document
				.write(multiFileEdit.archive[this.currentIndex].contents);
		generator.document.write("</textarea>");
		generator.document.write("</form>");
		generator.document.write('</body></html>');
		generator.document.getElementById("full-screen-edit-form").textarea
				.focus();
		generator.document.close();
	}

	
}

function update_parent() {
	opener.document.getElementById("testForm").textarea.value = document
			.getElementById("full-screen-edit-form").textarea.value;
	opener.multiFileEdit.updateArchive()
}

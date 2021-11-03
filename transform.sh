#!/bin/bash
# transform CoNLL formats, see help note below

# NOTE: we expect necessary libraries to be provided by CoNLL-RDF, if that changes, this script may will break
# NOTE: this script is slow because it checks dependencies and performs compilations, revise for real applications


##########
# config #
##########
# adjust to your system

# set to your CoNLL-RDF home directory
CONLL_RDF=conll-rdf

# set to your java execs
JAVA=java
JAVAC=javac

##############################
# check system requirenments #
##############################

function is_installed {
	if ! whereis $1 >&/dev/null;	then
		return 1
	fi
	if whereis $1 | egrep -m 1 '^'$1'[^\n]*/' >& /dev/null; then
		return 0
	fi
	return 1
}

for req in git $JAVA $JAVAC ; do
	if ! is_installed $req; then
		echo error: did not find required shell '"'$req'"', please install it 1>&2
		exit 1
	fi
done

if ! is_installed rapper; then
	echo error: did not find required shell '"'rapper'"', please install 'raptor2-utils (http://librdf.org/raptor)' 1>&2
	exit 1
fi

########
# init #
########
# do not touch

# setup CoNLL-RDF
RUN=$CONLL_RDF/run.sh;
if [ ! -e $RUN ]; then
mkdir -p $CONLL_RDF >&/dev/null;
git clone --single-branch https://github.com/acoli-repo/conll-rdf.git $CONLL_RDF
fi;
EXTRACT=$RUN' CoNLLStreamExtractor'
UPDATE=$RUN' CoNLLRDFUpdater'
FORMAT=$RUN' CoNLLRDFFormatter'
OWL=$CONLL_RDF/owl/conll.ttl;
if [ $OSTYPE = "cygwin" ]; then
	OWL=`cygpath -wa $CONLL_RDF/owl`'\conll.ttl';
fi;
chmod u+x $RUN
chmod u+x $CONLL_RDF/compile.sh;

##################
# basic help msg #
##################
echo 'synopsis: '$0' [-help]' 1>&2;
echo '          '$0' SRC TGT [OWL]' 1>&2;
echo '  -help list all supported formats' 1>&2
echo '  SRC   source format' 1>&2;
echo '  TGT   target format' 1>&2;
echo '  OWL   CoNLL ontology, TTL format, defaults to '$OWL 1>&2;
echo 'read CoNLL data from stdin, write to stdout' 1>&2
echo 'transform from SRC format to TGT format according to OWL' 1>&2

if echo $1 | egrep . >&/dev/null; then
	###########
	# preproc #
	###########
	# do not touch
	$CONLL_RDF/compile.sh;

	FORMATS=$(rapper -i turtle $OWL 2>/dev/null | \
		grep '<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>.*#Dialect>' | \
		sed "s/>.*//g" | sed "s/.*#//" | sort -u)

	# determines the classpath
	HOME=`echo $0 | sed -e s/'[^\/]*$'//`'.';
	cd $HOME
	HOME=`pwd -P`;
	cd - >&/dev/null;

	TGT=$HOME/bin

	mkdir $TGT >&/dev/null;

	CLASSPATH=$TGT":"`find $CONLL_RDF/lib | perl -pe 's/\n/:/g;' | sed s/':$'//`;
	if [ $OSTYPE = "cygwin" ]; then
		TGT=`cygpath -wa $HOME/bin`;
		CLASSPATH=$TGT;
		for lib in `find $CONLL_RDF/lib`; do
			CLASSPATH=$CLASSPATH';'`cygpath -wa $lib`
		done;
	fi;

	# updates CoNLL-transform files if necessary
	JAVAS=$(
		cd $HOME;
		for java in `find  . | egrep '\.java$'`; do
			class=`echo $java | sed -e s/'src\/'/'bin\/'/ -e s/'java$'/'class'/;`
			if [ ! -e $class ]; then
				echo $java;
			else if [ $java -nt $class ]; then
				echo $java;
				fi;
			fi;
		done;
		)

	cd $HOME
		if echo $JAVAS | grep java >/dev/null; then
			$JAVAC -d $TGT -classpath $CLASSPATH $JAVAS;
		fi;
	cd - >&/dev/null

	TRANSFORM=org/acoli/conll/transform/Transformer

	#####################
	# extended help msg #
	#####################
	# -help flag, instead of processing

	if echo $1 | grep -i '-help' >&/dev/null; then
		echo $FORMATS | wc -w | sed s/'$'/' supported SRC and TGT formats: '/ 1>&2
		for format in $FORMATS; do
			echo '  '$format 1>&2
		done;
		echo 1>&2
	fi;

	##############
	# processing #
	##############
	# should not be combined with -help

	# check args
	if echo $2 | egrep . >& /dev/null ; then

		# check installation
			if [ ! -e $TGT/$TRANSFORM.class ]; then
				echo 'error: compilation failed, did not find '$TRANSFORM' class in' $TGT 1>&2;
			else

		# transform
				$JAVA -Dfile.encoding=UTF8 -classpath $CLASSPATH $TRANSFORM -help -silent -version 1 $1 $2 $OWL
				#could also add  -Dlog4j.configuration=file:'src/log4j.properties' for another log4j config
			fi;
	fi;
fi;

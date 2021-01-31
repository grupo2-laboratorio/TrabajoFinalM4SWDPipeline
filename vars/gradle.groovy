import pipeline.*

class PasosGradle {
    static def nombres() {      
      return ['buildTest','sonar','run','rest','nexus']
    }
}

class PasosGradleCI {
    static def nombres() {      
      return ['buildTest','sonar','nexusCIUpload', 'createRelease', 'gitMergeDevelop']
    }
}

class PasosGradleCIDevelop {
    static def nombres() {      
      return ['buildTest','sonar','nexusCIUpload', 'createRelease']
    }
}


class PasosGradleCIFeature {
    static def nombres() {      
      return ['buildTest','sonar','nexusCIUpload']
    }
}


class PasosGradleCD {
    static def nombres() {      
      return ['gitDiff', 'nexusDownload','runGradle','test','gitMergeMaster', 'gitMergeDevelop', 'gitTagMaster']
    }
}

def llamar_pasos_gradle(){
    def pasos = new PasosGradle()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_cd_gradle(){
    def pasos = new PasosGradleCD()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_ci_gradle(){
    def pasos = new PasosGradleCI()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_ci_gradle_develop(){
    def pasos = new PasosGradleCIDevelop()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_ci_gradle_feature(){
    def pasos = new PasosGradleCIFeature()
    def nombres = pasos.nombres()
    return nombres
}

def call(stgs,ci_cd){
  
    script{
      def pasos = new PasosGradle()
      def nombres = pasos.nombres()
      def stages = []

      if(ci_cd == 'ci'){
        pasos = new PasosGradleCI()
        nombres = pasos.nombres()
        stgs.each{
          if(nombres.indexOf(it) != -1 ){
            stages.add(it)
          }
        }
      }
      else if(ci_cd == 'cd'){
        pasos = new PasosGradleCD()
        nombres = pasos.nombres()
        stgs.each{
          if(nombres.indexOf(it) != -1 ){
            stages.add(it)
          }
        }
      }
      else{}

      stages.each{
        stage(it){
          try{
            "${it}"()
            env.STG_NAME = "${it}"
          }
          catch(Exception e){
            error("Stage ${it} tiene problemas: ${e} o no existe.")
          }
        }
      }
    }
}


def buildTest(){
    sh "gradle clean build"
}

def sonar(){
    def scannerHome = tool 'sonar'; // scanner
    withSonarQubeEnv('sonar') { // server
        sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${env.REPOSITORIO}-${env.GIT_BRANCH}-${BUILD_NUMBER} -Dsonar.java.binaries=build " 
    }
}

def nexusCIUpload(){
    sh "ls build/libs/"
    try{
    nexusArtifactUploader(
    nexusVersion: 'nexus3',
    protocol: 'http',
    nexusUrl: 'localhost:8081',
    groupId: 'com.devopsusach2020',
    version: env.VERSION_PACKAGE_CI,
    repository: 'test-nexus',
    credentialsId: 'nexus',
    artifacts: [
        [artifactId: 'DevOpsUsach2020',
        classifier: '',
        file: 'build/libs/DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar',
        type: 'jar']
    ]
    )
    }
    catch(Exception e){
        error("Stage tiene problemas: ${e} o no existe.")
    }
}

def nexusCDUpload(){
    nexusArtifactUploader(
    nexusVersion: 'nexus3',
    protocol: 'http',
    nexusUrl: 'localhost:8081',
    groupId: 'com.devopsusach2020',
    version: env.VERSION_PACKAGE_CD,
    repository: 'test-nexus',
    credentialsId: 'nexus',
    artifacts: [
        [artifactId: 'DevOpsUsach2020',
        classifier: '',
        file: 'DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar',
        type: 'jar']
    ]
    )
}

def nexusDownload(){
    sh 'curl -X GET -u admin:admin http://localhost:8081/repository/test-nexus/com/devopsusach2020/DevOpsUsach2020/' + env.VERSION_PACKAGE_CI + '/DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar -O'
}

def runGradle(){
    sh 'nohup bash gradle bootRun &'
}

def test(){
    sleep(time: 10, unit: 'SECONDS')
    sh 'curl -X GET "http://localhost:8081/rest/mscovid/test?msg=testing"'
}

def createRelease(){
        def git = new pipeline.git.GitMethods()
        if (git.checkIfBranchExists(env.RELEASE)){
            println 'rama existe'
            if(git.isBranchUpdated(env.GIT_BRANCH, env.RELEASE)){
                println 'la rama ya está creada y actualizada contra ' + env.GIT_BRANCH
            }else{
                println 'se va a eliminar y crear'
                git.deleteBranch(env.RELEASE)
                git.createBranch(env.RELEASE, env.GIT_BRANCH)
            }
        }
        else{
            println 'rama no existe asi que se crea'
            git.createBranch(env.RELEASE, env.GIT_BRANCH)
        }
}


def gitDiff(){
    def git = new pipeline.git.GitMethods()
    git.diffBranch('origin/main', env.GIT_BRANCH)
}


def gitMergeMaster(){
    def git = new pipeline.git.GitMethods()
    git.gitMergeMaster(env.GIT_BRANCH)
}


def gitMergeDevelop(){
    def git = new pipeline.git.GitMethods()
    git.gitMergeDevelop()
}


def gitTagMaster(){
    def git = new pipeline.git.GitMethods()
    git.gitTagMaster(env.TAG)
}


return this;
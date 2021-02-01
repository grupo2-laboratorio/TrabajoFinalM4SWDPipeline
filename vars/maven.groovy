import pipeline.*

class PasosMaven {
    static def nombres() {      
      return ['compile','junit']
    }
}

class PasosMavenCI {
    static def nombres() {      
      return ['compile','junit','postman']
    }
}

class PasosMavenCD {
    static def nombres() {      
      return ['nexusDownload','runStage','test']
    }
}

def llamar_pasos_ci_maven(){
    def pasos = new PasosMavenCI()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_cd_maven(){
    def pasos = new PasosMavenCD()
    def nombres = pasos.nombres()
    return nombres
}

def llamar_pasos_maven(){
    def pasos = new PasosMaven()
    def nombres = pasos.nombres()
    return nombres
}

def call(stgs,ci_cd){
  
    script{
      def pasos = new PasosMaven()
      def nombres = pasos.nombres()
      def stages = []

      if(ci_cd == 'ci'){
        pasos = new PasosMavenCI()
        nombres = pasos.nombres()
        stgs.each{
          echo it
          if(nombres.indexOf(it) != -1 ){
            stages.add(it)
          }
        }
      }
      else if(ci_cd == 'cd'){
        pasos = new PasosMavenCD()
        nombres = pasos.nombres()
        stgs.each{
          if(nombres.indexOf(it) != -1 ){
            stages.add(it)
          }
        }
      }
      else{
        error("ci_cd no valido")
      }

      
      stages.each{
        stage(it){
          echo it
          try{
            "${it}"()
          }
          catch(Exception e){
            error("Stage ${it} tiene problemas: ${e} o no existe.")
          }
        }
      }
    }
}


def compile(){
  sh 'nohup bash ./mvnw spring-boot:run &'
}


def junit(){
  sh './mvnw clean test -e'
}


def jar(){
  sh './mvnw clean package -e'
}


def sonarQube(){
  withSonarQubeEnv('sonar') {
      sh './mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar'
  }
}


def nexusCIUpload(){
  nexusArtifactUploader(
    nexusVersion: 'nexus3',
    protocol: 'http',
    nexusUrl: 'localhost:8081',
    groupId: 'com.devopsusach2020',
    version: env.VERSION_PACKAGE_CI,
    repository: 'test-nexus',
    credentialsId: 'nexus-dianela',
    artifacts: [
          [artifactId: 'DevOpsUsach2020',
          classifier: '',
          file: 'build/DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar',
          type: 'jar']
    ]
  )
}


def nexusCDUpload(){
  nexusArtifactUploader(
    nexusVersion: 'nexus3',
    protocol: 'http',
    nexusUrl: 'localhost:8081',
    groupId: 'com.devopsusach2020',
    version: env.VERSION_PACKAGE_CD,
    repository: 'test-nexus',
    credentialsId: 'nexus-dianela',
    artifacts: [
          [artifactId: 'DevOpsUsach2020',
          classifier: '',
          file: 'DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar',
          type: 'jar']
    ]
  )
}


def runStage(){
  sh 'nohup bash ./mvnw spring-boot:run &'
}


def test(){
  sleep(time: 10, unit: "SECONDS")
  sh 'curl -X GET "http://localhost:8081/rest/mscovid/test?msg=testing"'
}


def nexusDownload(String vers){
  sh 'curl -X GET -u admin:admin http://localhost:8081/repository/test-nexus/com/devopsusach2020/DevOpsUsach2020/' + env.VERSION_PACKAGE_CI + '/DevOpsUsach2020-' + env.VERSION_PACKAGE_CI + '.jar -O'
}

def createRelease(){
        def git = new pipeline.git.GitMethods()
        if (git.checkIfBranchExists('release-v1-0-0')){
            if(git.isBranchUpdated(env.GIT_BRANCH, 'release-v1-0-0')){
                println 'la rama ya está creada y actualizada contra ' + env.GIT_BRANCH
            }else{
                git.deleteBranch('release-v1-0-0')
                git.createBranch('release-v1-0-0', env.GIT_BRANCH)
            }
        }
        else{
            git.createBranch('release-v1-0-0', env.GIT_BRANCH)
        }
}

def postman(){
  def git = new pipeline.git.GitMethods()
  def repositorio_postman = 'https://github.com/grupo2-laboratorio/TrabajoFinalM4Postman.git .'
  //sh "rm -rf TrabajoFinalM4Postman"
  git.gitClone(repositorio_postman)
  sh "newman run TrabajoFinalM4Postman/Dxc.postman_collection.json"
}

return this;
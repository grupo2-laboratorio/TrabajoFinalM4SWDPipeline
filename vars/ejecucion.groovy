import pipeline.*

def call(){
    def pasos_maven = []
    def pasos_gradle = []
    def ci_cd = ''
    pipeline {
        agent any
        options {
        timeout(time: 120, unit: 'SECONDS') 
        }
        parameters {
        string defaultValue: '', description: '', name: 'stage', trim: false
        string defaultValue: '', description: '', name: 'versionNexusCI', trim: false
        string defaultValue: '', description: '', name: 'versionNexusCD', trim: false
        choice choices: ['gradle', 'maven'], description: '', name: 'herramienta'
        string defaultValue: '', description: '', name: 'release', trim: false
        string defaultValue: '', description: '', name: 'tag', trim: false
        }
        stages {
            stage('Validaciones') {
                steps {
                    script{
                       

                        // version a env
                        env.VERSION_PACKAGE_CI = params.versionNexusCI
                        env.VERSION_PACKAGE_CD = params.versionNexusCD

                        // tag name
                        env.TAG = params.tag

                        sh 'env'
                        // validar tipo y archivos minimos
                        def git = new pipeline.git.GitMethods()
                        if (params.herramienta == "maven"){
                            def exists = fileExists 'pom.xml'
                            if (exists) {
                                echo "ejecución maven"
                            } else {
                                error("no existen archivos mínimos para ejecución maven")
                            }
                        }
                        else if (params.herramienta == "gradle"){
                            def exists = fileExists 'build.gradle'
                            if (exists) {
                                echo "ejecución gradle"
                            } else {
                                error("no existen archivos mínimos para ejecución gradle")
                            }
                        }
                        else {
                            error("error de validacion de tipo. parámetro tipo ${params.herramienta} no es válido")
                        }

                        // validar rama
                        if (env.GIT_BRANCH == 'develop' || env.GIT_BRANCH.contains('feature')){
                            env.ci_cd = 'ci'
                        }
                        else if (env.GIT_BRANCH.contains('release')){
                            env.ci_cd = 'cd'
                        }
                        else{
                            error("rama a ejecutar no corresponde a ninguna conocida: develop, feature, release.")
                        }

                        // validar formato release
                        if (params.release ==~ /release-v\d+-\d+-\d+/){
                            env.RELEASE = params.release
                        }
                        else{
                            error("formato incorrecto de release")
                        }


                        // validar stage
                        if (params.stage == "") {
                            echo "ejecución de todos los stages"
                            if (params.tipo == "maven" && env.ci_cd == "ci"){
                                echo "ejecucion maven ci"
                                pasos_maven = maven.llamar_pasos_ci_maven()
                                pasos_maven.each{
                                    echo it
                                }
                            }
                            else if (params.herramienta == "maven" && env.ci_cd == "cd"){
                                pasos_maven = maven.llamar_pasos_cd_maven()
                            }
                            else if (params.herramienta == "gradle" && env.ci_cd == "cd"){
                                pasos_gradle = gradle.llamar_pasos_cd_gradle()
                            }
                            else{
                                // gradle ci se eligen los pasos segun develop y feature
                                if (env.GIT_BRANCH == 'develop'){
                                    pasos_gradle = gradle.llamar_pasos_ci_gradle_develop()
                                }
                                else if (env.GIT_BRANCH.contains('feature')){
                                    pasos_gradle = gradle.llamar_pasos_ci_gradle_feature()
                                }
                                else{
                                    error("rama a ejecutar no corresponde a ninguna conocida: develop, feature, release.")
                                }
                                
                            }
                        }
                        else if (params.stage.split(';').length > 0 ){
                            echo "ejecutar los siguientes stages"
                            def pasos_a_ejecutar = params.stage.split(';')
                            if (params.herramienta == "maven"){
                                try {
                                    mvn_stgs = maven.llamar_pasos()
                                    pasos_a_ejecutar.each { 
                                        echo "${it}"
                                        if(mvn_stgs.indexOf(it) != -1){
                                            pasos_maven.add(it)
                                        }
                                    }
                                    if(pasos_maven.size() == 0){
                                        error("sin pasos detectados")
                                    }
                                } catch (Exception e) {
                                    echo 'Exception occurred: ' + e.toString()
                                    sh 'Handle the exception!'
                                }
                            }
                            else {
                                try{
                                    gradle_stgs = gradle.llamar_pasos()
                                    pasos_a_ejecutar.each { 
                                        echo "${it}"
                                        if(gradle_stgs.indexOf(it) != -1){
                                            try {
                                                pasos_gradle.add(it)
                                            } catch (Exception e) {
                                                echo 'Exception occurred: ' + e.toString()
                                                sh 'Handle the exception!'
                                            }
                                        }
                                    }
                                    if(pasos_gradle.size() == 0){
                                        error("sin pasos detectados")
                                    }
                                } catch (Exception e) {
                                    echo 'Exception occurred: ' + e.toString()
                                    sh 'Handle the exception!'
                                }
                            }

                        }
                         else {
                            error("error de validacion de stage. parámetro stage ${params.stage} no es válido")
                        }



                        def git_url = env.GIT_URL.split('/')
                        env.REPOSITORIO = git_url[-1].split('.git')[0]

                        // validar tecnologia ms, front, bff, etc
                        if(env.REPOSITORIO.startsWith('ms') || env.REPOSITORIO.startsWith('front') || env.REPOSITORIO.startsWith('bff')){
                            figlet 'tecnologia'
                            figlet env.REPOSITORIO 
                        }
                        else{
                            error("no se detecta tecnologia")
                        }
                        
                        figlet params.herramienta
                        figlet env.ci_cd

                    }
                }

            }
            stage('Pipeline') {
                steps {
                    script{
                        wrap([$class: 'BuildUser']) {
                            def user = env.BUILD_USER_ID
                        }

                        if (params.herramienta == 'maven') {
                            maven.call(pasos_maven,env.ci_cd)
 
                        } else {
                            gradle.call(pasos_gradle,env.ci_cd)
                        }
                    }
                }
            }
        } 
        post {
            success {
                slackSend (color: '#00FF00', message: "Build Success: [Grupo2][${env.JOB_NAME}][Rama: ${env.GIT_BRANCH}][Stage: [${env.STG_NAME}][Resultado: Ok]")
            }
            failure {
            slackSend (color: '#FF0000', message: "Build Failure: [[Grupo2][${env.JOB_NAME}][Rama: ${env.GIT_BRANCH}][Stage: [${env.STG_NAME}][Resultado: No Ok]")
            }
        }
    }

}
return this;
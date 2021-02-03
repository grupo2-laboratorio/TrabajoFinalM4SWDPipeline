package pipeline.git

def call(){

}

def gitClone(String repository){
    sh "git init"
    sh "git clone ${repository}"
    return true
}

def checkIfBranchExists(String branch){
    sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
    sh "git pull; git ls-remote --heads origin ${branch}"
    def output = sh (script: "git pull; git ls-remote --heads origin ${branch}", returnStdout: true)
    def respuesta = (!output?.trim()) ? true : false

    return respuesta
}

def isBranchUpdated(String ramaOrigen, String ramaDestino){
    sh "git checkout ${ramaOrigen}; git pull"
    sh "git checkout ${ramaDestino}; git pull"
    sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
    def output = sh (script: "git pull; git log origin/${ramaDestino}..origin/${ramaOrigen}", returnStdout: true)
    def respuesta = (!output?.trim()) ? true : false

    return respuesta
}

def deleteBranch(String branch){
    sh "git pull; git push origin --delete ${branch}"
}

def createBranch(String branch, String ramaOrigen){
    //sh "git reset --hard HEAD"
    
    sh "git pull"
    //sh "git branch -D ${branch}"
    sh "git checkout ${ramaOrigen}"
    sh "git checkout -b ${branch}"
    sh "git push origin ${branch}"

    //sh '''
    //    git checkout 
    //    ''' + ramaOrigen + '''
    //    git checkout -b ''' + branch + '''
    //    git push origin ''' + branch + '''
    //'''

}

def diffBranch(String targetBranch, String sourceBranch){
    sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
    sh "git --no-pager diff --name-status ${targetBranch} ${sourceBranch}"
}


def checkIfFileExists(String fileName){
    sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
    def output = sh (script: "git ls-files ${fileName}", returnStdout: true)
    def respuesta = (!output?.trim()) ? true : false

    return respuesta
}

def gitMergeMaster(String sourceBranch){
     sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
     sh "git fetch --all"
     sh "git checkout main"
     sh "git merge origin/${env.RELEASE} -m 'Merged ${sourceBranch} branch to main '  "
     sh "git push origin main"

}


def gitMergeDevelop(){
     sh "git config --add remote.origin.fetch +refs/heads/develop:refs/remotes/origin/develop"
     sh "git fetch --all"
     sh "git checkout develop"
     sh "git merge origin/main -m 'Merged ${env.RELEASE} branch to develop '  "
     sh "git push origin develop"
}


def gitMergeDevelopDeprecado(){
     sh "git config --add remote.origin.fetch +refs/heads/main:refs/remotes/origin/main"
     sh "git config --add remote.origin.fetch +refs/heads/develop:refs/remotes/origin/develop"
     sh "git fetch --all"
     sh "git pull"
     sh "git checkout develop"
     sh "git merge origin/${env.GIT_BRANCH} -m 'Merged ${env.GIT_BRANCH} branch to develop '"
     sh "git push origin develop"
}


def gitTagMaster(String version){

    sh "git tag ${version}"
    sh "git push origin ${version}"

}


return this;

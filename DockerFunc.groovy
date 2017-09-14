#!groovy

 
int excuteTime = 30
try{
    excuteTime=env.maxExcutiontime .toInteger()}
catch(exc){
    excuteTime= 30
}
echo "excuteTime="+excuteTime
timeout(excuteTime){

        
        stage('代码下载') {
            try {
                creid = "${SCMcredential}"
                //  echo  'CREDENTIALS'
                //  echo creid
            } catch (exc) {
                echo "Caught: ${exc}"
                creid = ""
            }
            //if (creid != null && creid != "") {
            echo ' credential id' + creid
            if (env.SCMTYPE == "git") {
                scmtype = 'GitSCM'
                echo 'using credential to git'
                git credentialsId: creid, url: env.SCMUrl, branch: env.SCMBranch
            } else { //svn
                echo 'using credential to svn'
                scmtype = 'SubversionSCM'
                checkout([$class: scmtype, additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: creid, depthOption: 'infinity', ignoreExternalsOption: true, local: '.', branches: [[name: '*/' + env.SCMBranch]],          \
                       remote: env.SCMUrl]], workspaceUpdater: [$class: 'UpdateUpdater']])
            }
//+env.SCMBranch
        }
         
        if (env.Dockercompile == "true") {
            def imagename=env.Dockercompile_image
            echo imagename
            stage("Docker编译") {
                def para=""
                if (env.Dockercompile_dockerworkdir!=""){
                    para+="--workdir  "+env.Dockercompile_dockerworkdir
                }

                if (env.Dockercompile_srcMap!=""){
                    para+="-v  `pwd`:"+env.Dockercompile_srcMap
                }
                
                if (env.Dockercompile_dockeruser!=""){
                    para+="-u "+env.Dockercompile_dockeruser
                }
                if (env.dockercompile_distDirMap!=""){
                    para+="-v "+env.dockercompile_distDirMap
                }

                
                sh "docker run --rm -it ${para} ${imagename} ${dockercompile_dockercmd}"
            }
        }
         
         
        //def imagename=env.CreateImage_registry+'/'+env.CreateImage_tag+":"+env.BUILD_TAG 
        //    echo imagename
        def imagename=env.CreateImage_registry+'/'+env.CreateImage_tag+":"+env.BUILD_TAG 

        stage("创建镜像") {

                sh "docker build -t ${imagename} . -f ${CreateImage_dockerfile}"
                
                 sh " echo '${imagename}' >dockerbuildresult.txt"
                    dirname="dockerbuildtempdir"+	System.currentTimeMillis()		
                    sh "mkdir '${dirname}' "
                    filename = java.net.URLEncoder.encode("image--"+imagename, "UTF-8")+".zip"
                    
                    zip archive: true, dir:  dirname , glob: '', zipFile: filename
                    
        }
        
        stage("Push镜像") {
                    sh "docker push   '${imagename}' "
        }
            
         if (env.DeployToRancher_skip == "false") {
            

                    stage("部署应用") {
                    //sh "/usr/bin/rancher.sh ${DeployToRancher_arg}  '${DeployToRancher_service}' '${imagename}' ${DeployToRancher_cmd} "
                                    sh " export DeployToRancher_arg='${DeployToRancher_arg}'; \
                                    export DeployToRancher_cmd='${DeployToRancher_cmd}'; \
                                    export DeployToRancher_service='${DeployToRancher_service}'; \
                                    export imagename=${imagename}; \
                                    export DeployToRancher_environment=${DeployToRancher_environment}; \
                                    /usr/bin/rancher.sh "
                }

               
                
            }
        
 
}

return this
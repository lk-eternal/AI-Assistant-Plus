#!/bin/bash

work_path="/apps/images"
git_url="https://gitee.com/of/AI-Assistant-Plus.git"
project_dir="/apps/AI-Assistant-Plus"
jar_file_name="ai-assistant-plus"
JAVA_OPTS="-Dfile.encoding=UTF-8"
docker_service_name="lk-ai"
check_interval=10

function git_clone_or_update {
	if [ ! -d "$project_dir" ]; then
		git clone "$git_url" "$project_dir"
		return 0
	else
		cd "$project_dir" || exit
		if git pull | grep -q 'Already up to date.'; then
			return 1
		else
			return 0
		fi
	fi
}

function build_maven_project {
	cd "$project_dir" || exit
	mvn clean package -f pom.xml -DskipTests
}

function get_latest_jar_version {
	echo $(find "$project_dir/target" -type f -name "$jar_file_name-*.jar" | sort -V | tail -n 1 | sed 's/.*\([0-9]\+\(\.[0-9]\+\)\{2\}\).*/\1/')
}

function build_docker_image {
	version=$1
	cd "$work_path" || exit
	docker build -t "$docker_service_name:$version" .
}

function restart_docker_service {
	version=$1
	for ((i=1; i<=2; i++)); do
		service_name=$docker_service_name-$i;
		if docker ps -aqf "name=$service_name"; then
			docker rm -f $service_name
		fi
		docker run -d --name "$service_name" \
		--restart unless-stopped \
		-v "$project_dir/target/$jar_file_name-$version.jar:/app/app.jar" \
		-v "$work_path/application.properties:/app/application.properties" \
		-p 808$i:8080 \
		-e JAVA_OPTS="$JAVA_OPTS" \
		$docker_service_name:$version
	done
}

function check_for_updates {
	latest_start_status=0
	while true; do
		echo "克隆/更新代码仓库..."
		git_clone_or_update
		if [ $? -ne 0 ] && [ $latest_start_status -eq 1 ]; then
			#没有更新且上一次部署成功
			echo "没有更新"
			sleep $check_interval
			continue
		fi

		latest_start_status=0

		echo "开始构建应用..."
		build_maven_project
		if [ $? -ne 0 ]; then
			echo "构建应用失败"
			sleep $check_interval
			continue
		fi

		latest_version=$(get_latest_jar_version)
		echo "构建成功,最新版本:$latest_version"

		echo "开始构建镜像..."
		build_docker_image $latest_version
		if [ $? -ne 0 ]; then
			echo "构建镜像失败"
			sleep $check_interval
			continue
		fi

		echo "开始启动..."
		restart_docker_service $latest_version
		echo "启动完成"

		latest_start_status=1

		docker ps -af name=$docker_service_name

		sleep $check_interval
		continue
	done
}

check_for_updates
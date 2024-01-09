#!/bin/bash

JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

current_dir="$(pwd)"
git_url="https://gitee.com/of/AI-Assistant-Plus.git"
project_dir="/apps/run/AI-Assistant-Plus"
jar_file_name="ai-assistant-plus-0.1.0.jar"
jar_file_path="$project_dir/target/$jar_file_name"
check_interval=10

# 检查是否存在Java进程
java_pids=$(jps -l | grep $jar_file_name | awk '{print $1}')
# 关闭相关Java进程（如果需要的话）
for pid in $java_pids; do
    kill -9 "$pid" > /dev/null 2>&1
    echo "已关闭相关Java进程。"
done

function check_for_updates {
  has_update=1

  # 克隆或更新 Git 仓库
  if [ ! -d "$project_dir" ]; then
    echo "克隆 Git 仓库..."
    git clone "$git_url" "$project_dir"
  else
    cd "$project_dir" || exit
    git pull | grep -q 'Already up to date.' && has_update=0
  fi

  # 检查是否存在Java进程
  java_pids=$(jps -l | grep $jar_file_name | awk '{print $1}')

  if [ "$has_update" -eq 0 ] && [ -n "$java_pids" ]; then
      echo "没有更新且Java应用已运行,无需重新部署"
      sleep $check_interval
      check_for_updates
      return
  fi

  echo "开始构建应用..."
  cd "$project_dir" || exit
  mvn -DskipTests clean install -f "$project_dir/pom.xml"

  # 关闭相关Java进程（如果需要的话）
  for pid in $java_pids; do
      kill -9 "$pid" > /dev/null 2>&1
      echo "已关闭相关Java进程。"
  done

  echo "开始启动应用..."
  cp "$jar_file_path" "$current_dir"
  cd "$current_dir" || exit
  sudo bash -c "nohup java $JAVA_TOOL_OPTIONS -jar $current_dir/$jar_file_name >/dev/null 2>&1 &"
  echo "已启动应用"

  sleep $check_interval
  check_for_updates
}

# 运行检查更新函数
check_for_updates

read -p "按回车键退出..."
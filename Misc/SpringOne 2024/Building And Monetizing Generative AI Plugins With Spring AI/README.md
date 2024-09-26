# Goal
Goal is to take user prompt, to use the same model to translate the prompt to vector and then to perform vector similarity search over my local database
By doing that, we can find some relevant movie recommendation

## Reference
Based on SpringOne 2024 talk - https://www.youtube.com/watch?v=UmeoLny4nSk&ab_channel=SpringDeveloper

Guide from the talk - https://github.com/YugabyteDB-Samples/openai-gpt-plugin-pgvector
    Forked repo - https://github.com/vrmehta93/openai-gpt-plugin-pgvector/tree/main


## Notes

Clone the forked repo in the current directory
Using Postgres option and docker compose option
    So no changes required in any of the source files

Updated devcontainer.js
    Install node and npm

Docker:
    To see containers:`docker ps -a`
    To remove containers: docker rm <hash>
    To stop a container: docker stop <hash>
    To stop all containers: docker stop $(docker ps -a -q)
    To remove all stopped containers: docker container prune
    To restart container: docker restart <hash>

Added .devcontainer/startupscript.sh
    To make it executable, from system terminal:
        chmod +x .devcontainer/startupscript.sh

In postgres,
    Each movie has an associated vector
        This was generated with the "embedded models" of openAI
        Loop of ready every "overview" column and generating a vector for "overview_vector" column
        Model defined in `spring.ai.openai.embedding.model`


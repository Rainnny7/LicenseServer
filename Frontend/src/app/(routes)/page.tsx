import { Button } from "../components/ui/button";
import {
	ContextMenu,
	ContextMenuContent,
	ContextMenuItem,
	ContextMenuTrigger,
} from "../components/ui/context-menu";

const Home = (): JSX.Element => (
	<div className="flex flex-col gap-5">
		<h1>Hello World</h1>

		<ContextMenu>
			<ContextMenuTrigger>
				<Button>Click Me</Button>
			</ContextMenuTrigger>
			<ContextMenuContent>
				<ContextMenuItem>Profile</ContextMenuItem>
				<ContextMenuItem>Billing</ContextMenuItem>
				<ContextMenuItem>Team</ContextMenuItem>
				<ContextMenuItem>Subscription</ContextMenuItem>
			</ContextMenuContent>
		</ContextMenu>
	</div>
);
export default Home;
